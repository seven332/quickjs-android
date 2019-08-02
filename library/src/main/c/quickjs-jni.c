#include <jni.h>
#include <quickjs.h>
#include <malloc.h>
#include <string.h>

#include "java-helper.h"

#define MSG_OOM "Out of memory"
#define MSG_NULL_JS_RUNTIME "Null JSRuntime"
#define MSG_NULL_JS_CONTEXT "Null JSContext"
#define MSG_NULL_JS_VALUE "Null JSValue"

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createRuntime(JNIEnv *env, jclass clazz) {
    jlong runtime = (jlong) JS_NewRuntime();
    CHECK_NULL_RET(env, runtime, MSG_OOM)
    return runtime;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyRuntime(JNIEnv *env, jclass clazz, jlong runtime) {
    JSRuntime *rt = (JSRuntime *) runtime;
    CHECK_NULL(env, rt, MSG_NULL_JS_RUNTIME)
    JS_FreeRuntime(rt);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createContext(JNIEnv *env, jclass clazz, jlong runtime) {
    JSRuntime *rt = (JSRuntime *) runtime;
    CHECK_NULL_RET(env, rt, MSG_NULL_JS_RUNTIME);
    jlong context = (jlong) JS_NewContext(rt);
    CHECK_NULL_RET(env, context, MSG_OOM);
    return context;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyContext(JNIEnv *env, jclass clazz, jlong context) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL(env, ctx, MSG_NULL_JS_CONTEXT);
    JS_FreeContext(ctx);
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueTag(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return JS_VALUE_GET_NORM_TAG(*val);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_isValueArray(JNIEnv *env, jclass clazz, jlong context, jlong value) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return (jboolean) JS_IsArray(ctx, *val);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_isValueFunction(JNIEnv *env, jclass clazz, jlong context, jlong value) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    return (jboolean) JS_IsFunction(ctx, *val);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_invokeValueFunction(JNIEnv *env, jclass clazz,
        jlong context, jlong function, jlong thisObj, jlongArray args) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *func_obj = (JSValue *) function;
    CHECK_NULL_RET(env, func_obj, "Null function");
    JSValue *this_obj = (JSValue *) thisObj;
    CHECK_NULL_RET(env, args, "Null arguments")
    jlong *elements = (*env)->GetLongArrayElements(env, args, NULL);
    CHECK_NULL_RET(env, elements, MSG_OOM);

    int argc = (*env)->GetArrayLength(env, args);
    JSValueConst argv[argc];
    for (int i = 0; i < argc; i++) {
        argv[i] = *((JSValue *) elements[i]);
    }

    jlong result = 0;

    JSValue ret = JS_Call(ctx, *func_obj, this_obj != NULL ? *this_obj : JS_UNDEFINED, argc, argv);

    // Make a copy of ret
    void *copy = malloc(sizeof(JSValue));
    if (copy != NULL) {
        memcpy(copy, &ret, sizeof(JSValue));
        result = (jlong) copy;
    } else {
        // Free ret now due to failing to make a copy
        JS_FreeValue(ctx, ret);
    }

    (*env)->ReleaseLongArrayElements(env, args, elements, JNI_ABORT);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueProperty__JJI(JNIEnv *env, jclass clazz, jlong context, jlong value, jint index) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);

    jlong result = 0;

    JSValue prop = JS_GetPropertyUint32(ctx, *val, (uint32_t) index);

    // Make a copy of prop
    void *copy = malloc(sizeof(JSValue));
    if (copy != NULL) {
        memcpy(copy, &prop, sizeof(JSValue));
        result = (jlong) copy;
    } else {
        // Free prop now due to failing to make a copy
        JS_FreeValue(ctx, prop);
    }

    CHECK_NULL_RET(env, result, MSG_OOM);

    return result;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueProperty__JJLjava_lang_String_2(JNIEnv *env, jclass clazz, jlong context, jlong value, jstring name) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_NULL_RET(env, name, "Null name");

    const char *name_utf = (*env)->GetStringUTFChars(env, name, NULL);
    CHECK_NULL_RET(env, name_utf, MSG_OOM);

    jlong result = 0;

    JSValue prop = JS_GetPropertyStr(ctx, *val, name_utf);

    // Make a copy of prop
    void *copy = malloc(sizeof(JSValue));
    if (copy != NULL) {
        memcpy(copy, &prop, sizeof(JSValue));
        result = (jlong) copy;
    } else {
        // Free prop now due to failing to make a copy
        JS_FreeValue(ctx, prop);
    }

    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    CHECK_NULL_RET(env, result, MSG_OOM);

    return result;
}

#define CHECK_JS_TAG_RET(VAL, TARGET, TYPE)                                                    \
    int32_t __tag__ = JS_VALUE_GET_NORM_TAG(VAL);                                              \
    if (__tag__ != (TARGET)) {                                                                 \
        THROW_JS_DATA_EXCEPTION_RET(env, "Invalid JSValue tag for %s: %d", (TYPE), __tag__);   \
    }

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueBoolean(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_BOOL, "boolean");
    return (jboolean) (JS_VALUE_GET_BOOL(*val));
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueInt(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_INT, "int");
    return (jint) (JS_VALUE_GET_INT(*val));
}

JNIEXPORT jdouble JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueDouble(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL_RET(env, val, MSG_NULL_JS_VALUE);
    CHECK_JS_TAG_RET(*val, JS_TAG_FLOAT64, "double");
    return (jdouble) JS_VALUE_GET_FLOAT64(*val);
}

JNIEXPORT jstring JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueString(JNIEnv *env, jclass clazz, jlong context, jlong value) {
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

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyValue(JNIEnv *env, jclass clazz, jlong context, jlong value) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL(env, ctx, MSG_NULL_JS_CONTEXT);
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, val, MSG_NULL_JS_VALUE);
    JS_FreeValue(ctx, *val);
    free(val);
}

JNIEXPORT jobject JNICALL
Java_com_hippo_quickjs_android_QuickJS_getException(JNIEnv *env, jclass clazz, jlong context) {
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
Java_com_hippo_quickjs_android_QuickJS_evaluate(JNIEnv *env, jclass clazz,
        jlong context, jstring source_code, jstring file_name, jint flags) {
    JSContext *ctx = (JSContext *) context;
    CHECK_NULL_RET(env, ctx, MSG_NULL_JS_CONTEXT);
    CHECK_NULL_RET(env, source_code, "Null source code");
    CHECK_NULL_RET(env, file_name, "Null file name");

    const char *source_code_utf = NULL;
    jsize source_code_length = 0;
    const char *file_name_utf = NULL;
    jlong result = 0;

    source_code_utf = (*env)->GetStringUTFChars(env, source_code, NULL);
    source_code_length = (*env)->GetStringUTFLength(env, source_code);
    file_name_utf = (*env)->GetStringUTFChars(env, file_name, NULL);

    if (source_code_utf != NULL && file_name_utf != NULL) {
        JSValue val = JS_Eval(ctx, source_code_utf, (size_t) source_code_length, file_name_utf, flags);

        // Make a copy of val
        void *copy = malloc(sizeof(JSValue));
        if (copy != NULL) {
            memcpy(copy, &val, sizeof(JSValue));
            result = (jlong) copy;
        } else {
            // Free val now due to failing to make a copy
            JS_FreeValue(ctx, val);
        }
    }

    if (source_code_utf != NULL) {
        (*env)->ReleaseStringUTFChars(env, source_code, source_code_utf);
    }
    if (file_name_utf != NULL) {
        (*env)->ReleaseStringUTFChars(env, file_name, file_name_utf);
    }

    CHECK_NULL_RET(env, result, MSG_OOM);

    return result;
}
