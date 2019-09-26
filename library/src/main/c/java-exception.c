#include <stdio.h>

#include "java-exception.h"

#define MAX_MSG_SIZE 1024

static jclass js_evaluation_exception_class;
static jmethodID js_evaluation_exception_constructor;

void throw_exception(JNIEnv *env, const char *exception_name, const char *message, ...) {
    char formatted_message[MAX_MSG_SIZE];
    va_list va_args;
    va_start(va_args, message);
    vsnprintf(formatted_message, MAX_MSG_SIZE, message, va_args);
    va_end(va_args);

    jclass exception_class = (*env)->FindClass(env, exception_name);
    if (exception_class == NULL) {
        exception_class = (*env)->FindClass(env, CLASS_NAME_ILLEGAL_ARGUMENT_EXCEPTION);
        snprintf(formatted_message, MAX_MSG_SIZE, "Can't find class: %s", exception_name);
    }

    (*env)->ThrowNew(env, exception_class, formatted_message);
}

void throw_JSEvaluationException(JNIEnv *env, JSContext *ctx) {
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

    jobject throwable = (*env)->NewObject(
            env,
            js_evaluation_exception_class,
            js_evaluation_exception_constructor,
            is_error,
            exception_j_str,
            stack_j_str
    );
    CHECK_NULL_IA_RET(env, throwable, "Can't create instance of JSEvaluationException");

    (*env)->Throw(env, throwable);
}

bool java_exception_init(JNIEnv *env) {
    js_evaluation_exception_class = (*env)->FindClass(env, "com/hippo/quickjs/android/JSEvaluationException");
    js_evaluation_exception_class = (*env)->NewGlobalRef(env, js_evaluation_exception_class);
    if (js_evaluation_exception_class == NULL) return false;

    js_evaluation_exception_constructor = (*env)->GetMethodID(env, js_evaluation_exception_class, "<init>", "(ZLjava/lang/String;Ljava/lang/String;)V");
    if (js_evaluation_exception_constructor == NULL) return false;

    return true;
}
