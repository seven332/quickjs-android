#include <jni.h>
#include <quickjs.h>
#include <string.h>
#include <malloc.h>

#include "pickle.h"
#include "unpickle.h"
#include "java-exception.h"
#include "java-method.h"
#include "java-object.h"
#include "java-helper.h"

#define DEFAULT_SINK_SIZE 16
#define ERROR_MSG_SIZE 256

static jmethodID on_interrupt_method;

typedef struct InterruptData {
    JavaVM *vm;
    jobject interrupt_handler;
} InterruptData;

typedef struct QJRuntime {
    JSRuntime *rt;
    InterruptData *interrupt_date;
} QJRuntime;

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createRuntime(JNIEnv *env, jclass clazz) {
    QJRuntime *qj_rt = malloc(sizeof(QJRuntime));
    CHECK_NULL_OOM_RET_STH(env, qj_rt, 0);
    JSRuntime *rt = JS_NewRuntime();
    CHECK_NULL_OOM_RET_STH(env, rt, 0);
    qj_rt->rt = rt;
    qj_rt->interrupt_date = NULL;
    return (jlong) qj_rt;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_setRuntimeMallocLimit(JNIEnv *env, jclass clazz, jlong runtime, jint malloc_limit) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    JS_SetMemoryLimit(qj_rt->rt, (size_t) malloc_limit);
}

static int on_interrupt(JSRuntime *rt, void *opaque) {
    int result = 0;

    InterruptData *data = opaque;

    OBTAIN_ENV(data->vm);

    if (unlikely(env != NULL)) {
        result = (*env)->CallBooleanMethod(env, data->interrupt_handler, on_interrupt_method);
        if (unlikely((*env)->ExceptionCheck(env))) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            result = 0;
        }
    }

    RELEASE_ENV(data->vm);

    return result;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_setRuntimeInterruptHandler(JNIEnv *env, jclass clazz, jlong runtime, jobject interrupt_handler) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;

    InterruptData *data = qj_rt->interrupt_date;

    if (interrupt_handler == NULL) {
        // Clear interrupt handler
        if (data != NULL) {
            (*env)->DeleteGlobalRef(env, data->interrupt_handler);
            js_free_rt(qj_rt->rt, data);
            qj_rt->interrupt_date = NULL;
            JS_SetInterruptHandler(qj_rt->rt, NULL, NULL);
        }
    } else {
        // Set interrupt handler
        if (data == NULL) {
            data = js_malloc_rt(qj_rt->rt, sizeof(InterruptData));
            CHECK_NULL_OOM_RET(env, data);
        } else {
            (*env)->DeleteGlobalRef(env, data->interrupt_handler);
            data->vm = NULL;
            data->interrupt_handler = NULL;
        }

        (*env)->GetJavaVM(env, &(data->vm));
        data->interrupt_handler = (*env)->NewGlobalRef(env, interrupt_handler);

        qj_rt->interrupt_date = data;
        JS_SetInterruptHandler(qj_rt->rt, on_interrupt, data);
    }
}

#ifdef LEAK_TRIGGER

static int leak_state = 0;

// Redirect printf() to this function
// to get memory leak detection result in JS_FreeRuntime()
// without modifying the source code of QuickJS.
int leak_trigger(const char* _, ...) {
    leak_state = 1;
    return 0;
}

#endif

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyRuntime(JNIEnv *env, jclass clazz, jlong runtime) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    JSRuntime *rt = qj_rt->rt;

    InterruptData *data = qj_rt->interrupt_date;
    if (data != NULL) {
        (*env)->DeleteGlobalRef(env, data->interrupt_handler);
        js_free_rt(rt, data);
    }

#ifdef LEAK_TRIGGER
    leak_state = 0;
#endif
    JS_FreeRuntime(rt);
#ifdef LEAK_TRIGGER
    if (leak_state != 0) {
        throw_exception(env, CLASS_NAME_ILLEGAL_STATE_EXCEPTION, "Memory Leak");
    }
#endif
    free(qj_rt);
}

void throw_error(int error_type, const char* error_msg, JNIEnv *env, JSContext *ctx) {
    if (error_type == ERROR_TYPE_JS_EVALUATION) {
        throw_JSEvaluationException(env, ctx);
    } else {
        const char *exception_name;
        switch (error_type) {
            case ERROR_TYPE_OOM:
                exception_name = CLASS_NAME_OUT_OF_MEMORY_ERROR;
                error_msg = EMPTY_STRING;
                break;
            case ERROR_TYPE_JS_DATA:
                exception_name = CLASS_NAME_JS_DATA_EXCEPTION;
                break;
            case ERROR_TYPE_ILLEGAL_ARGUMENT:
                exception_name = CLASS_NAME_ILLEGAL_ARGUMENT_EXCEPTION;
                break;
            case ERROR_TYPE_ILLEGAL_STATE:
            default:
                exception_name = CLASS_NAME_ILLEGAL_STATE_EXCEPTION;
                break;
        }
        throw_exception(env, exception_name, error_msg);
    }
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createContext(JNIEnv *env, jclass clazz, jlong runtime) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    JSRuntime *rt = qj_rt->rt;

    JSContext *ctx = JS_NewContext(rt);
    CHECK_NULL_OOM_RET_STH(env, ctx, 0);

    // TODO
//    if (java_method_init_context(ctx)) THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);
//    if (java_object_init_context(ctx)) THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);

    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyContext(JNIEnv *env, jclass clazz, jlong context) {
    JSContext *ctx = (JSContext *) context;
    JS_FreeContext(ctx);
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_setContextValue(
        JNIEnv *env,
        jclass clazz,
        jlong context,
        jstring name,
        jlong unpickle_command,
        jbyteArray bytes,
        jint byte_size
) {
    JSContext *ctx = (JSContext *) context;
    void *command_ptr = (void *) unpickle_command;

    // Unpickle
    void *source_ptr = js_malloc_rt(JS_GetRuntime(ctx), (size_t) byte_size);
    CHECK_NULL_OOM_RET(env, source_ptr);
    (*env)->GetByteArrayRegion(env, bytes, 0, byte_size, source_ptr);
    BitSource source = CREATE_BIT_SOURCE(source_ptr, (size_t) byte_size);
    BitSource command = CREATE_COMMAND_BIT_SOURCE(command_ptr);
    JSValue val = unpickle(ctx, &command, &source);
    js_free_rt(JS_GetRuntime(ctx), source_ptr);

    if (JS_IsException(val)) THROW_JS_EVALUATION_EXCEPTION_RET(env, ctx);

    const char *name_utf_8 = (*env)->GetStringUTFChars(env, name, NULL);
    if (unlikely(name_utf_8 == NULL)) {
        JS_FreeValue(ctx, val);
        THROW_OUT_OF_MEMORY_ERROR_RET(env);
    }

    JSValue global_obj = JS_GetGlobalObject(ctx);
    JS_SetPropertyStr(ctx, global_obj, name_utf_8, val);
    JS_FreeValue(ctx, global_obj);
    (*env)->ReleaseStringUTFChars(env, name, name_utf_8);
}

static jbyteArray bit_sink_to_jbyte_array(JNIEnv *env, BitSink *sink) {
    size_t length = bit_sink_get_length(sink);
    jbyteArray array = (*env)->NewByteArray(env, length);
    if (unlikely(array == NULL)) return NULL;
    void *data = bit_sink_get_data(sink);
    (*env)->SetByteArrayRegion(env, array, 0, length, data);
    return array;
}

JNIEXPORT jbyteArray JNICALL
Java_com_hippo_quickjs_android_QuickJS_invokeValueFunction(
        JNIEnv *env,
        jclass clazz,
        jlong context,
        jlong value,
        jstring name,
        jlongArray unpickle_commands,
        jobjectArray arg_contexts,
        jintArray arg_context_sizes,
        jlong pickle_command,
        jboolean required
) {
    JSContext *ctx = (JSContext *) context;
    JSValue *val = (JSValue *) value;

    // Get function
    const char *name_utf8 = (*env)->GetStringUTFChars(env, name, NULL);
    CHECK_NULL_OOM_RET_STH(env, name_utf8, NULL);
    JSValue prop = JS_GetPropertyStr(ctx, *val, name_utf8);
    (*env)->ReleaseStringUTFChars(env, name, name_utf8);
    if (JS_IsException(prop)) THROW_JS_EVALUATION_EXCEPTION_RET_STH(env, ctx, NULL);
    if (!JS_IsFunction(ctx, prop)) {
        JS_FreeValue(ctx, prop);
        if (required) THROW_ILLEGAL_ARGUMENT_EXCEPTION_RET_STH(env, NULL, "The property is not a function");
        else return NULL;
    }

    // Convert to JSValue
    jint arg_count = (*env)->GetArrayLength(env, unpickle_commands);
    JSValue args[arg_count];
    if (arg_count > 0) {
        jint arg_context_size_array[arg_count];
        (*env)->GetIntArrayRegion(env, arg_context_sizes, 0, arg_count, arg_context_size_array);

        // Malloc arg context buffer
        jint arg_context_max_size = 0;
        for (jint i = 0; i < arg_count; i++) {
            jint size = arg_context_size_array[i];
            if (size > arg_context_max_size) arg_context_max_size = size;
        }
        void *arg_context_buffer = js_malloc_rt(JS_GetRuntime(ctx), (size_t) arg_context_max_size);
        if (arg_context_buffer == NULL) {
            JS_FreeValue(ctx, prop);
            THROW_OUT_OF_MEMORY_ERROR_RET_STH(env, NULL);
        }

        jlong commands[arg_count];
        (*env)->GetLongArrayRegion(env, unpickle_commands, 0, arg_count, commands);

        jint index = 0;
        for (; index < arg_count; index++) {
            jbyteArray arg_context_bytes = (*env)->GetObjectArrayElement(env, arg_contexts, index);
            (*env)->GetByteArrayRegion(env, arg_context_bytes, 0, arg_context_size_array[index], arg_context_buffer);
            BitSource command = CREATE_COMMAND_BIT_SOURCE(commands[index]);
            BitSource source = CREATE_BIT_SOURCE(arg_context_buffer, (size_t) arg_context_size_array[index]);
            args[index] = unpickle(ctx, &command, &source);
            if (JS_IsException(args[index])) break;
        }

        js_free_rt(JS_GetRuntime(ctx), arg_context_buffer);

        // Catch JS exception
        if (index != arg_count) {
            // Free generated JSValues
            for (jint i = 0; i <= index; i++) {
                JS_FreeValue(ctx, args[i]);
            }
            JS_FreeValue(ctx, prop);
            THROW_JS_EVALUATION_EXCEPTION_RET_STH(env, ctx, NULL);
        }
    }

    JSValue ret = JS_Call(ctx, prop, *val, arg_count, args);

    for (jint i = 0; i < arg_count; i++) {
        JS_FreeValue(ctx, args[i]);
    }
    JS_FreeValue(ctx, prop);

    if (JS_IsException(ret)) THROW_JS_EVALUATION_EXCEPTION_RET_STH(env, ctx, NULL);

    // Pickle ret
    BitSource source = CREATE_COMMAND_BIT_SOURCE(pickle_command);
    BitSink sink;
    if (!create_bit_sink(&sink, DEFAULT_SINK_SIZE)) {
        JS_FreeValue(ctx, ret);
        THROW_OUT_OF_MEMORY_ERROR_RET_STH(env, NULL);
    }
    int error_type = ERROR_TYPE_JS_EVALUATION;
    char error_msg[ERROR_MSG_SIZE];
    pickle(ctx, ret, &source, &sink, &error_type, error_msg, ERROR_MSG_SIZE);

    jbyteArray result = bit_sink_to_jbyte_array(env, &sink);
    destroy_bit_sink(&sink);

    CHECK_NULL_OOM_RET_STH(env, result, NULL);
    return result;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyValue(JNIEnv *env, jclass clazz, jlong context, jlong value) {
    JSContext *ctx = (JSContext *) context;
    JSValue *val = (JSValue *) value;
    JS_FreeValue(ctx, *val);
    js_free_rt(JS_GetRuntime(ctx), val);
}

JNIEXPORT jbyteArray JNICALL
Java_com_hippo_quickjs_android_QuickJS_evaluate(
        JNIEnv *env,
        jclass clazz,
        jlong context,
        jstring source_code,
        jstring file_name,
        jint flags,
        jlong pickle_pointer
) {
    JSContext *ctx = (JSContext *) context;

    BitSource source;
    BitSink sink;

    if (pickle_pointer != 0) {
        void *pickler = (void *) pickle_pointer;
        source = CREATE_COMMAND_BIT_SOURCE(pickler);
        if (unlikely(!create_bit_sink(&sink, DEFAULT_SINK_SIZE))) THROW_OUT_OF_MEMORY_ERROR_RET_STH(env, NULL);
    }

    const char *source_code_utf = (*env)->GetStringUTFChars(env, source_code, NULL);
    jsize source_code_length = (*env)->GetStringUTFLength(env, source_code);
    const char *file_name_utf = (*env)->GetStringUTFChars(env, file_name, NULL);

    if (unlikely(source_code_utf == NULL || file_name_utf == NULL)) {
        if (pickle_pointer != 0) destroy_bit_sink(&sink);
        if (source_code_utf != NULL) (*env)->ReleaseStringUTFChars(env, source_code, source_code_utf);
        if (file_name_utf != NULL) (*env)->ReleaseStringUTFChars(env, file_name, file_name_utf);
        THROW_OUT_OF_MEMORY_ERROR_RET_STH(env, NULL);
    }

    JSValue val = JS_Eval(ctx, source_code_utf, (size_t) source_code_length, file_name_utf, flags);
    (*env)->ReleaseStringUTFChars(env, source_code, source_code_utf);
    (*env)->ReleaseStringUTFChars(env, file_name, file_name_utf);

    if (JS_IsException(val)) {
        throw_JSEvaluationException(env, ctx);
        return NULL;
    }

    if (pickle_pointer == 0) {
        // No pickle pointer
        // Return null now
        JS_FreeValue(ctx, val);
        return NULL;
    }

    int error_type = ERROR_TYPE_JS_EVALUATION;
    char error_msg[ERROR_MSG_SIZE];
    if (unlikely(!pickle(ctx, val, &source, &sink, &error_type, error_msg, ERROR_MSG_SIZE))) {
        throw_error(error_type, error_msg, env, ctx);
        return NULL;
    }

    jbyteArray result = bit_sink_to_jbyte_array(env, &sink);
    CHECK_NULL_OOM_RET_STH(env, result, NULL);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_pushCommand(JNIEnv *env, jclass clazz, jbyteArray command) {
    jsize size = (*env)->GetArrayLength(env, command);
    void *copy = malloc(sizeof(jsize) + size);
    CHECK_NULL_OOM_RET_STH(env, copy, 0);
    *(jsize *) copy = size;
    (*env)->GetByteArrayRegion(env, command, 0, size, copy + sizeof(jsize));
    return (jlong) copy;
}

JNIEXPORT jlongArray JNICALL
Java_com_hippo_quickjs_android_QuickJS_pushCommands(JNIEnv *env, jclass clazz, jobjectArray commands) {
    jsize command_size = (*env)->GetArrayLength(env, commands);
    jlong pointers[command_size];

    for (jsize i = 0; i < command_size; i++) {
        jbyteArray command = (*env)->GetObjectArrayElement(env, commands, i);
        jsize size = (*env)->GetArrayLength(env, command);

        void *copy = malloc(sizeof(jsize) + size);
        if (unlikely(copy == NULL)) {
            // Free previous pointers
            for (int j = 0; j < i; j++) {
                free((void *) pointers[j]);
            }
            THROW_OUT_OF_MEMORY_ERROR_RET_STH(env, NULL);
        }

        *(jsize *) copy = size;
        (*env)->GetByteArrayRegion(env, command, 0, size, copy + sizeof(jsize));

        pointers[i] = (jlong) copy;
    }

    jlongArray result = (*env)->NewLongArray(env, command_size);
    (*env)->SetLongArrayRegion(env, result, 0, command_size, pointers);
    return result;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_updateCommands(JNIEnv *env, jclass clazz, jlongArray pointers, jobjectArray commands) {
    jsize command_size = (*env)->GetArrayLength(env, commands);
    jlong pointer_array[command_size];
    (*env)->GetLongArrayRegion(env, pointers, 0, command_size, pointer_array);

    for (jsize i = 0; i < command_size; i++) {
        jbyte *pointer = (jbyte *) pointer_array[i];
        jbyteArray command = (*env)->GetObjectArrayElement(env, commands, i);
        jsize size = (*env)->GetArrayLength(env, command);
        (*env)->GetByteArrayRegion(env, command, 0, size, pointer + sizeof(jsize));
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_popCommands(JNIEnv *env, jclass clazz, jlongArray pointers) {
    jsize pointer_size = (*env)->GetArrayLength(env, pointers);
    jlong pointer_array[pointer_size];
    (*env)->GetLongArrayRegion(env, pointers, 0, pointer_size, pointer_array);

    for (jsize i = 0; i < pointer_size; i++) {
        void *pointer = (jbyte *) pointer_array[i];
        free(pointer);
    }
}

static bool init(JNIEnv *env) {
    jclass interrupt_handler_clazz = (*env)->FindClass(env, "com/hippo/quickjs/android/JSRuntime$InterruptHandler");
    if (interrupt_handler_clazz == NULL) return false;
    on_interrupt_method = (*env)->GetMethodID(env, interrupt_handler_clazz, "onInterrupt", "()Z");
    if (on_interrupt_method == NULL) return false;
    return true;
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void* reserved) {
    JNIEnv *env = NULL;

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    if (!init(env)) return JNI_ERR;
    if (!java_exception_init(env)) return JNI_ERR;
    if (java_method_init(env)) return JNI_ERR;

    return JNI_VERSION_1_6;
}
