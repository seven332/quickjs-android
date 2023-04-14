#include <jni.h>
#include <quickjs.h>
#include <string.h>
#include <malloc.h>

#include "java-method.h"
#include "java-object.h"
#include "java-helper.h"

#define MSG_OOM "Out of memory"
#define MSG_NULL_JS_RUNTIME "Null JSRuntime"
#define MSG_NULL_JS_CONTEXT "Null JSContext"
#define MSG_NULL_JS_VALUE "Null JSValue"

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
Java_com_hippo_quickjs_android_QuickJS_createRuntime(JNIEnv *env, jclass __unused clazz) {
    QJRuntime *qj_rt = malloc(sizeof(QJRuntime));
    CHECK_NULL_RET(env, qj_rt, MSG_OOM);
    JSRuntime *rt = JS_NewRuntime();
    CHECK_NULL_RET(env, rt, MSG_OOM);
    qj_rt->rt = rt;
    qj_rt->interrupt_date = NULL;
    return (jlong) qj_rt;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_setRuntimeMallocLimit(
    JNIEnv *env,
    jclass __unused clazz,
    jlong runtime,
    jint malloc_limit
) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    CHECK_NULL(env, qj_rt, MSG_NULL_JS_RUNTIME);
    JS_SetMemoryLimit(qj_rt->rt, (size_t) malloc_limit);
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_setRuntimeMaxStackSize(
    JNIEnv *env,
    jclass __unused clazz,
    jlong runtime,
    jint stack_size
) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    CHECK_NULL(env, qj_rt, MSG_NULL_JS_RUNTIME);
    JS_SetMaxStackSize(qj_rt->rt, (size_t) stack_size);
}

static int on_interrupt(JSRuntime __unused *rt, void *opaque) {
    int result = 0;

    InterruptData *data = opaque;

    OBTAIN_ENV(data->vm);

    if (env != NULL) {
        result = (*env)->CallBooleanMethod(env, data->interrupt_handler, on_interrupt_method);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            result = 0;
        }
    }

    RELEASE_ENV(data->vm);

    return result;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_setRuntimeInterruptHandler(
    JNIEnv *env,
    jclass __unused clazz,
    jlong runtime,
    jobject interrupt_handler
) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    CHECK_NULL(env, qj_rt, MSG_NULL_JS_RUNTIME);

    InterruptData *data = qj_rt->interrupt_date;

    if (interrupt_handler == NULL) {
        // Clear interrupt handler
        if (data != NULL) {
            (*env)->DeleteGlobalRef(env, data->interrupt_handler);
            free(data);
            qj_rt->interrupt_date = NULL;
            JS_SetInterruptHandler(qj_rt->rt, NULL, NULL);
        }
    } else {
        // Set interrupt handler
        if (data == NULL) {
            data = malloc(sizeof(InterruptData));
            CHECK_NULL(env, data, MSG_OOM);
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
Java_com_hippo_quickjs_android_QuickJS_destroyRuntime(
    JNIEnv *env,
    jclass __unused clazz,
    jlong runtime
) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    CHECK_NULL(env, qj_rt, MSG_NULL_JS_RUNTIME);
    JSRuntime *rt = qj_rt->rt;
#ifdef LEAK_TRIGGER
    leak_state = 0;
#endif
    JS_FreeRuntime(rt);
#ifdef LEAK_TRIGGER
    if (leak_state != 0) {
        THROW_ILLEGAL_STATE_EXCEPTION(env, "Memory Leak");
    }
#endif
    InterruptData *data = qj_rt->interrupt_date;
    if (data != NULL) {
        (*env)->DeleteGlobalRef(env, data->interrupt_handler);
        free(data);
    }
    free(qj_rt);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createContext(
    JNIEnv *env,
    jclass __unused clazz,
    jlong runtime
) {
    QJRuntime *qj_rt = (QJRuntime *) runtime;
    CHECK_NULL_RET(env, qj_rt, MSG_NULL_JS_RUNTIME);
    JSRuntime *rt = qj_rt->rt;

    JSContext *ctx = JS_NewContext(rt);
    CHECK_NULL_RET(env, ctx, MSG_OOM);

    if (java_method_init_context(ctx)) THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);
    if (java_object_init_context(ctx)) THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);

    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyContext(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL(env, ctx, MSG_NULL_JS_CONTEXT);
    JS_FreeContext(ctx);
}

#define COPY_JS_VALUE(JS_CONTEXT, JS_VALUE, RESULT)                                    \
    do {                                                                               \
        void *__copy__ = js_malloc_rt(JS_GetRuntime(JS_CONTEXT), sizeof(JSValue));     \
        if (__copy__ != NULL) {                                                        \
            memcpy(__copy__, &(JS_VALUE), sizeof(JSValue));                            \
            (RESULT) = __copy__;                                                       \
        } else {                                                                       \
            JS_FreeValue((JS_CONTEXT), (JS_VALUE));                                    \
        }                                                                              \
    } while (0)

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueUndefined(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = 0;
    JSValue val = JS_UNDEFINED;
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueNull(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = JS_NULL;
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueBoolean(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jboolean value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = JS_NewBool(ctx, value);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueInt(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jint value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = JS_NewInt32(ctx, value);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueFloat64(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jdouble value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = JS_NewFloat64(ctx, value);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueString(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jstring value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    CHECK_NULL_RET(env, value, "Null value");

    const char *value_utf = (*env)->GetStringUTFChars(env, value, NULL);
    CHECK_NULL_RET(env, value_utf, MSG_OOM);

    JSValue *result = NULL;
    JSValue val = JS_NewString(ctx, value_utf);
    COPY_JS_VALUE(ctx, val, result);

    (*env)->ReleaseStringUTFChars(env, value, value_utf);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueObject(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = JS_NewObject(ctx);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueArray(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = JS_NewArray(ctx);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

#define CREATE_VALUE_ARRAY_BUFFER_METHOD(METHOD_NAME, JNI_ARRAY_TYPE, JNI_TYPE, COPY)     \
JNIEXPORT jlong JNICALL                                                                   \
METHOD_NAME(                                                                              \
    JNIEnv *env,                                                                          \
    jclass __unused clazz,                                                                \
    jlong context,                                                                        \
    JNI_ARRAY_TYPE array,                                                                 \
    jint start,                                                                           \
    jint length                                                                           \
) {                                                                                       \
    JSContext *ctx = (JSContext *) context;                                               \
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);                                        \
                                                                                          \
    size_t buffer_length = length * sizeof(JNI_TYPE);                                     \
    JNI_TYPE *buffer = malloc(buffer_length);                                             \
    CHECK_NULL_RET(env, buffer, MSG_OOM);                                                 \
                                                                                          \
    (*env)->COPY(env, array, start, length, buffer);                                      \
    if ((*env)->ExceptionCheck(env)) {                                                    \
        free(buffer);                                                                     \
        return 0;                                                                         \
    }                                                                                     \
                                                                                          \
    JSValue *result = NULL;                                                               \
    JSValue val = JS_NewArrayBufferCopy(ctx, buffer, buffer_length);                      \
    COPY_JS_VALUE(ctx, val, result);                                                      \
    free(buffer);                                                                         \
    CHECK_NULL_RET(env, result, MSG_OOM);                                                 \
                                                                                          \
    return (jlong) result;                                                                \
}

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferZ,
    jbooleanArray,
    jboolean,
    GetBooleanArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferB,
    jbyteArray,
    jbyte,
    GetByteArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferC,
    jcharArray,
    jchar,
    GetCharArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferS,
    jshortArray,
    jshort,
    GetShortArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferI,
    jintArray,
    jint,
    GetIntArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferJ,
    jlongArray,
    jlong,
    GetLongArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferF,
    jfloatArray,
    jfloat,
    GetFloatArrayRegion
)

CREATE_VALUE_ARRAY_BUFFER_METHOD(
    Java_com_hippo_quickjs_android_QuickJS_createValueArrayBufferD,
    jdoubleArray,
    jdouble,
    GetDoubleArrayRegion
)

static jlong createValueFunction(
    JNIEnv *env,
    jlong context,
    jobject js_context,
    jboolean is_static,
    jobject callee,
    jstring method_name,
    jstring method_sign,
    jobject return_type,
    jobjectArray arg_types,
    jboolean is_callback_method
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    const char *method_name_utf8 = (*env)->GetStringUTFChars(env, method_name, NULL);
    const char *method_sign_utf8 = (*env)->GetStringUTFChars(env, method_sign, NULL);
    if (method_name_utf8 == NULL || method_sign_utf8 == NULL) {
        if (method_name_utf8 != NULL) (*env)->ReleaseStringUTFChars(env, method_name, method_name_utf8);
        if (method_sign_utf8 != NULL) (*env)->ReleaseStringUTFChars(env, method_sign, method_sign_utf8);
        THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);
    }

    jmethodID method = NULL;
    if (is_static) {
        method = (*env)->GetStaticMethodID(env, callee, method_name_utf8, method_sign_utf8);
    } else {
        jclass callee_class = (*env)->GetObjectClass(env, callee);
        method = (*env)->GetMethodID(env, callee_class, method_name_utf8, method_sign_utf8);
    }
    (*env)->ReleaseStringUTFChars(env, method_name, method_name_utf8);
    (*env)->ReleaseStringUTFChars(env, method_sign, method_sign_utf8);
    if (method == NULL) {
        if ((*env)->ExceptionCheck(env)) return 0;
        THROW_ILLEGAL_STATE_EXCEPTION_RET(env, "Can't find method");
    }

    int arg_count = (*env)->GetArrayLength(env, arg_types);
    jobject arg_types_copy[arg_count];
    for (int i = 0; i < arg_count; i++) {
        arg_types_copy[i] = (*env)->GetObjectArrayElement(env, arg_types, i);
    }

    JSValue *result = NULL;
    JSValue val = QJ_NewJavaMethod(ctx, env, js_context, is_static, callee, method, return_type, arg_count, arg_types_copy, is_callback_method);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueFunction(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jobject js_context,
    jobject instance,
    jstring method_name,
    jstring method_sign,
    jobject return_type,
    jobjectArray arg_types,
    jboolean is_callback_method
) {
    return createValueFunction(env, context, js_context, JNI_FALSE, instance, method_name, method_sign, return_type, arg_types, is_callback_method);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueFunctionS(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jobject js_context,
    jstring class_name,
    jstring method_name,
    jstring method_sign,
    jobject return_type,
    jobjectArray arg_types
) {
    const char *class_name_utf8 = (*env)->GetStringUTFChars(env, class_name, NULL);
    CHECK_NULL_RET(env, class_name_utf8, MSG_OOM);
    jclass callee = (*env)->FindClass(env, class_name_utf8);
    (*env)->ReleaseStringUTFChars(env, class_name, class_name_utf8);
    if (callee == NULL) {
        if ((*env)->ExceptionCheck(env)) return 0;
        THROW_ILLEGAL_STATE_EXCEPTION_RET(env, "Can't find class");
    }
    return createValueFunction(env, context, js_context, JNI_TRUE, callee, method_name, method_sign, return_type, arg_types, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValueJavaObject(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jobject object
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;
    JSValue val = QJ_NewJavaObject(ctx, env, object);
    COPY_JS_VALUE(ctx, val, result);
    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlongArray JNICALL
Java_com_hippo_quickjs_android_QuickJS_createValuePromise(
    JNIEnv *env,
    __unused jclass clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    jlongArray result = (*env)->NewLongArray(env, 3);
    CHECK_NULL_RET(env, result, MSG_OOM);

    JSValue functions[2] = { JS_UNDEFINED, JS_UNDEFINED };
    JSValue promise = JS_NewPromiseCapability(ctx, functions);

    JSValue *promise_result = NULL;
    JSValue *function1_result = NULL;
    JSValue *function2_result = NULL;
    COPY_JS_VALUE(ctx, promise, promise_result);
    if (promise_result == NULL) {
        JS_FreeValue(ctx, functions[0]);
        JS_FreeValue(ctx, functions[1]);
        THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);
    }
    COPY_JS_VALUE(ctx, functions[0], function1_result);
    if (function1_result == NULL) {
        JS_FreeValue(ctx, *promise_result);
        js_free_rt(JS_GetRuntime(ctx), promise_result);
        JS_FreeValue(ctx, functions[1]);
        THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);
    }
    COPY_JS_VALUE(ctx, functions[1], function2_result);
    if (function2_result == NULL) {
        JS_FreeValue(ctx, *promise_result);
        js_free_rt(JS_GetRuntime(ctx), promise_result);
        JS_FreeValue(ctx, *function1_result);
        js_free_rt(JS_GetRuntime(ctx), function1_result);
        THROW_ILLEGAL_STATE_EXCEPTION_RET(env, MSG_OOM);
    }

    (*env)->SetLongArrayRegion(env, result, 0, 1, (const jlong *) &promise_result);
    (*env)->SetLongArrayRegion(env, result, 1, 1, (const jlong *) &function1_result);
    (*env)->SetLongArrayRegion(env, result, 2, 1, (const jlong *) &function2_result);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_defineValueProperty__JJIJI(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value,
    jint index,
    jlong property,
    jint flags
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    JSValue *prop = (JSValue *) property;
    CHECK_NULL_RET(env, prop, "Null property");

    JS_DupValue(ctx, *prop);

    return (jboolean) (JS_DefinePropertyValueUint32(ctx, *val, (uint32_t) index, *prop, flags) >= 0);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_defineValueProperty__JJLjava_lang_String_2JI(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value,
    jstring name,
    jlong property,
    jint flags
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    JSValue *prop = (JSValue *) property;
    CHECK_NULL_RET(env, prop, "Null property");

    const char *name_utf = (*env)->GetStringUTFChars(env, name, NULL);
    CHECK_NULL_RET(env, name_utf, MSG_OOM);

    JS_DupValue(ctx, *prop);

    jboolean result = (jboolean) (JS_DefinePropertyValueStr(ctx, *val, name_utf, *prop, flags) >= 0);

    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueTag(
    JNIEnv *env,
    jclass __unused clazz,
    jlong value
) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return JS_VALUE_GET_NORM_TAG(*val);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_isValueArray(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return (jboolean) JS_IsArray(ctx, *val);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_isValueArrayBuffer(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return (jboolean) JS_IsArrayBuffer(ctx, *val);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_isValueFunction(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return (jboolean) JS_IsFunction(ctx, *val);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_invokeValueFunction(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong function,
    jlong thisObj,
    jlongArray args
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *func_obj = (JSValue *) function;
    CHECK_NULL_RET(env, func_obj, "Null function");
    JSValue *this_obj = (JSValue *) thisObj;
    CHECK_NULL_RET(env, args, "Null arguments");
    jlong *elements = (*env)->GetLongArrayElements(env, args, NULL);
    CHECK_NULL_RET(env, elements, MSG_OOM);

    int argc = (*env)->GetArrayLength(env, args);
    JSValueConst argv[argc];
    for (int i = 0; i < argc; i++) {
        argv[i] = *((JSValue *) elements[i]);
    }

    JSValue *result = NULL;

    JSValue ret = JS_Call(ctx, *func_obj, this_obj != NULL ? *this_obj : JS_UNDEFINED, argc, argv);

    COPY_JS_VALUE(ctx, ret, result);

    (*env)->ReleaseLongArrayElements(env, args, elements, JNI_ABORT);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueProperty__JJI(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value,
    jint index
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);

    JSValue *result = NULL;

    JSValue prop = JS_GetPropertyUint32(ctx, *val, (uint32_t) index);

    COPY_JS_VALUE(ctx, prop, result);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueProperty__JJLjava_lang_String_2(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value,
    jstring name
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_NULL_RET(env, name, "Null name");

    const char *name_utf = (*env)->GetStringUTFChars(env, name, NULL);
    CHECK_NULL_RET(env, name_utf, MSG_OOM);

    JSValue *result = NULL;

    JSValue prop = JS_GetPropertyStr(ctx, *val, name_utf);

    COPY_JS_VALUE(ctx, prop, result);

    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_setValueProperty__JJIJ(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value,
    jint index,
    jlong property
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    JSValue *prop = (JSValue *) property;
    CHECK_NULL_RET(env, prop, "Null property");

    // JS_SetPropertyUint32 requires a reference count of the property JSValue
    // Meanwhile, it calls JS_FreeValue on the property JSValue if it fails
    JS_DupValue(ctx, *prop);

    return (jboolean) (JS_SetPropertyUint32(ctx, *val, (uint32_t) index, *prop) >= 0);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_setValueProperty__JJLjava_lang_String_2J(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value,
    jstring name,
    jlong property
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_NULL_RET(env, name, "Null name");
    JSValue *prop = (JSValue *) property;
    CHECK_NULL_RET(env, prop, "Null property");

    const char *name_utf = (*env)->GetStringUTFChars(env, name, NULL);
    CHECK_NULL_RET(env, name_utf, MSG_OOM);

    // JS_SetPropertyStr requires a reference count of the property JSValue
    // Meanwhile, it calls JS_FreeValue on the property JSValue if it fails
    JS_DupValue(ctx, *prop);

    jboolean result = (jboolean) (JS_SetPropertyStr(ctx, *val, name_utf, *prop) >= 0);

    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    return result;
}

#define TO_ARRAY(METHOD_NAME, JNI_ARRAY_TYPE, JNI_TYPE, TYPE_BYTES, NEW_METHOD, GET_METHOD, RELEASE_METHOD) \
JNIEXPORT JNI_ARRAY_TYPE JNICALL                                                                            \
METHOD_NAME(                                                                                                \
    JNIEnv *env,                                                                                            \
    jclass clazz,                                                                                           \
    jlong context,                                                                                          \
    jlong value                                                                                             \
) {                                                                                                         \
    JSContext *ctx = (JSContext *) context;                                                                 \
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);                                                          \
    JSValue *val = (JSValue *) value;                                                                       \
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);                                                            \
                                                                                                            \
    size_t size = 0;                                                                                        \
    uint8_t *buffer = JS_GetArrayBuffer(ctx, &size, *val);                                                  \
    CHECK_NULL_RET(env, buffer, "No buffer");                                                               \
    CHECK_FALSE_RET(env, size % (TYPE_BYTES) == 0, "Size not matched");                                     \
                                                                                                            \
    JNI_ARRAY_TYPE array = (*env)->NEW_METHOD(env, size / (TYPE_BYTES));                                    \
    CHECK_NULL_RET(env, array, MSG_OOM);                                                                    \
                                                                                                            \
    JNI_TYPE *elements = (*env)->GET_METHOD(env, array, NULL);                                              \
    CHECK_NULL_RET(env, elements, MSG_OOM);                                                                 \
                                                                                                            \
    memcpy(elements, buffer, size);                                                                         \
                                                                                                            \
    (*env)->RELEASE_METHOD(env, array, elements, JNI_COMMIT);                                               \
                                                                                                            \
    return array;                                                                                           \
}

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toBooleanArray,
    jbooleanArray,
    jboolean,
    1,
    NewBooleanArray,
    GetBooleanArrayElements,
    ReleaseBooleanArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toByteArray,
    jbyteArray,
    jbyte,
    1,
    NewByteArray,
    GetByteArrayElements,
    ReleaseByteArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toCharArray,
    jcharArray,
    jchar,
    2,
    NewCharArray,
    GetCharArrayElements,
    ReleaseCharArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toShortArray,
    jshortArray,
    jshort,
    2,
    NewShortArray,
    GetShortArrayElements,
    ReleaseShortArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toIntArray,
    jintArray,
    jint,
    4,
    NewIntArray,
    GetIntArrayElements,
    ReleaseIntArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toLongArray,
    jlongArray,
    jlong,
    8,
    NewLongArray,
    GetLongArrayElements,
    ReleaseLongArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toFloatArray,
    jfloatArray,
    jfloat,
    4,
    NewFloatArray,
    GetFloatArrayElements,
    ReleaseFloatArrayElements
)

TO_ARRAY(
    Java_com_hippo_quickjs_android_QuickJS_toDoubleArray,
    jdoubleArray,
    jdouble,
    8,
    NewDoubleArray,
    GetDoubleArrayElements,
    ReleaseDoubleArrayElements
)

#define CHECK_JS_TAG_RET(VAL, TARGET, TYPE)                                                        \
    do {                                                                                           \
        int32_t __tag__ = JS_VALUE_GET_NORM_TAG(VAL);                                              \
        if (__tag__ != (TARGET)) {                                                                 \
            THROW_JS_DATA_EXCEPTION_RET(env, "Invalid JSValue tag for %s: %d", (TYPE), __tag__);   \
        }                                                                                          \
    } while (0)

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueBoolean(
    JNIEnv *env,
    jclass __unused clazz,
    jlong value
) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_BOOL, "boolean");
    return (jboolean) (JS_VALUE_GET_BOOL(*val));
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueInt(
    JNIEnv *env,
    jclass __unused clazz,
    jlong value
) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_INT, "int");
    return (jint) (JS_VALUE_GET_INT(*val));
}

JNIEXPORT jdouble JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueFloat64(
    JNIEnv *env,
    jclass __unused clazz,
    jlong value
) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_FLOAT64, "float64");
    return (jdouble) JS_VALUE_GET_FLOAT64(*val);
}

JNIEXPORT jstring JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueString(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_STRING, "string");

    const char *str = JS_ToCString(ctx, *val);
    CHECK_NULL_RET(env, str, MSG_OOM);

    jstring j_str = (*env)->NewStringUTF(env, str);

    JS_FreeCString(ctx, str);

    CHECK_NULL_RET(env, j_str, MSG_OOM);

    return j_str;
}

JNIEXPORT jobject JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueJavaObject(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return QJ_GetJavaObject(ctx, *val);
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyValue(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jlong value
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, val, MSG_NULL_JS_VALUE);
    JS_FreeValue(ctx, *val);
    js_free_rt(JS_GetRuntime(ctx), val);
}

JNIEXPORT jobject JNICALL
Java_com_hippo_quickjs_android_QuickJS_getException(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    jclass js_exception_class = (*env)->FindClass(env, "com/hippo/quickjs/android/JSException");
    CHECK_NULL_RET(env, js_exception_class, "Can't find JSException");

    jmethodID constructor_id = (*env)->GetMethodID(env, js_exception_class, "<init>", "(ZLjava/lang/String;Ljava/lang/String;)V");
    CHECK_NULL_RET(env, constructor_id, "Can't find JSException constructor");

    const char *exception_str = NULL;
    const char *stack_str = NULL;

    JSValue exception = JS_GetException(ctx);
    exception_str = JS_ToCString(ctx, exception);
    jboolean is_error = (jboolean) JS_IsError(ctx, exception);
    if (is_error) {
        JSValue stack = JS_GetPropertyStr(ctx, exception, "stack");
        if (!JS_IsUndefined(stack)) {
            stack_str = JS_ToCString(ctx, stack);
        }
        JS_FreeValue(ctx, stack);
    }
    JS_FreeValue(ctx, exception);

    jstring exception_j_str = (exception_str != NULL) ? (*env)->NewStringUTF(env, exception_str) : NULL;
    jstring stack_j_str = (stack_str != NULL) ? (*env)->NewStringUTF(env, stack_str) : NULL;

    if (exception_str != NULL) {
        JS_FreeCString(ctx, exception_str);
    }
    if (stack_str != NULL) {
        JS_FreeCString(ctx, stack_str);
    }

    jobject result = (*env)->NewObject(env, js_exception_class, constructor_id, is_error, exception_j_str, stack_j_str);
    CHECK_NULL_RET(env, result, "Can't create instance of JSException");

    return result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_getGlobalObject(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    JSValue *result = NULL;

    JSValue val = JS_GetGlobalObject(ctx);
    COPY_JS_VALUE(ctx, val, result);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_evaluate(
    JNIEnv *env,
    jclass __unused clazz,
    jlong context,
    jstring source_code,
    jstring file_name,
    jint flags
) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    CHECK_NULL_RET(env, source_code, "Null source code");
    CHECK_NULL_RET(env, file_name, "Null file name");

    const char *source_code_utf = NULL;
    jsize source_code_length = 0;
    const char *file_name_utf = NULL;
    JSValue *result = NULL;

    source_code_utf = (*env)->GetStringUTFChars(env, source_code, NULL);
    source_code_length = (*env)->GetStringUTFLength(env, source_code);
    file_name_utf = (*env)->GetStringUTFChars(env, file_name, NULL);

    if (source_code_utf != NULL && file_name_utf != NULL) {
        JSValue val = JS_Eval(ctx, source_code_utf, (size_t) source_code_length, file_name_utf, flags);

        COPY_JS_VALUE(ctx, val, result);
    }

    if (source_code_utf != NULL) {
        (*env)->ReleaseStringUTFChars(env, source_code, source_code_utf);
    }
    if (file_name_utf != NULL) {
        (*env)->ReleaseStringUTFChars(env, file_name, file_name_utf);
    }

    CHECK_NULL_RET(env, result, MSG_OOM);

    return (jlong) result;
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_executePendingJob(JNIEnv *env, jclass clazz, jlong context) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);

    // if the context has pending jobs，jobCtx will be ctx
    // don't destroy it
    JSContext *jobCtx;
    return JS_ExecutePendingJob(JS_GetRuntime(ctx), &jobCtx);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void __unused * reserved) {
    JNIEnv *env = NULL;

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass interrupt_handler_clazz = (*env)->FindClass(env, "com/hippo/quickjs/android/JSRuntime$InterruptHandler");
    if (interrupt_handler_clazz == NULL) {
        return JNI_ERR;
    }
    on_interrupt_method = (*env)->GetMethodID(env, interrupt_handler_clazz, "onInterrupt", "()Z");
    if (on_interrupt_method == NULL) {
        return JNI_ERR;
    }

    if (java_method_init(env)) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
