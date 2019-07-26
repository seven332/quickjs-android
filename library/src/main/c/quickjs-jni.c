#include <jni.h>
#include <quickjs.h>
#include <malloc.h>
#include <string.h>

#include "java-helper.h"

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createRuntime(JNIEnv *env, jclass clazz) {
    return (jlong) JS_NewRuntime();
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyRuntime(JNIEnv *env, jclass clazz, jlong runtime) {
    JSRuntime *rt = (JSRuntime *) runtime;
    if (rt != NULL) {
        JS_FreeRuntime(rt);
    }
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_createContext(JNIEnv *env, jclass clazz, jlong runtime) {
    JSRuntime *rt = (JSRuntime *) runtime;
    if (rt == NULL) {
        return 0;
    }
    return (jlong) JS_NewContext(rt);
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyContext(JNIEnv *env, jclass clazz, jlong context) {
    JSContext *ctx = (JSContext *) context;
    if (ctx != NULL) {
        JS_FreeContext(ctx);
    }
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueTag(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, val, "Null JSValue pointer");
    return JS_VALUE_GET_NORM_TAG(*val);
}

#define CHECK_JS_TAG(VAL, TARGET, TYPE)                                                        \
    int32_t __tag__ = JS_VALUE_GET_NORM_TAG(VAL);                                              \
    if (__tag__ != (TARGET)) {                                                                 \
        THROW_ILLEGAL_STATE_EXCEPTION(env, "Invalid JSValue tag for %s: %d", (TYPE), __tag__); \
    }

JNIEXPORT jboolean JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueBoolean(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, val, "Null JSValue pointer");
    CHECK_JS_TAG(*val, JS_TAG_BOOL, "boolean");
    return (jboolean) (JS_VALUE_GET_BOOL(*val));
}

JNIEXPORT jint JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueInt(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, val, "Null JSValue pointer");
    CHECK_JS_TAG(*val, JS_TAG_INT, "int");
    return (jint) (JS_VALUE_GET_INT(*val));
}

JNIEXPORT jdouble JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueDouble(JNIEnv *env, jclass clazz, jlong value) {
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, val, "Null JSValue pointer");
    CHECK_JS_TAG(*val, JS_TAG_FLOAT64, "float64");
    return (jdouble) JS_VALUE_GET_FLOAT64(*val);
}

JNIEXPORT jstring JNICALL
Java_com_hippo_quickjs_android_QuickJS_getValueString(JNIEnv *env, jclass clazz, jlong context, jlong value) {
    JSContext *ctx = (JSContext *) context;
    JSValue *val = (JSValue *) value;
    CHECK_NULL(env, ctx, "Null JSContext pointer");
    CHECK_NULL(env, val, "Null JSValue pointer");
    CHECK_JS_TAG(*val, JS_TAG_STRING, "string");

    const char *str = JS_ToCString(ctx, *val);
    CHECK_NULL(env, str, "Out of memory");

    jstring j_str = (*env)->NewStringUTF(env, str);

    JS_FreeCString(ctx, str);

    CHECK_NULL(env, j_str, "Out of memory");

    return j_str;
}

JNIEXPORT void JNICALL
Java_com_hippo_quickjs_android_QuickJS_destroyValue(JNIEnv *env, jclass clazz, jlong context, jlong value) {
    JSContext *ctx = (JSContext *) context;
    JSValue *val = (JSValue *) value;
    if (ctx != NULL && val != NULL) {
        JS_FreeValue(ctx, *val);
        free(val);
    }
}

JNIEXPORT jlong JNICALL
Java_com_hippo_quickjs_android_QuickJS_evaluate(JNIEnv *env, jclass clazz,
        jlong context, jstring source_code, jstring file_name, jint flags) {
    JSContext *ctx = (JSContext *) context;
    if (ctx == NULL || source_code == NULL || file_name == NULL) {
        return 0;
    }

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
    return result;
}
