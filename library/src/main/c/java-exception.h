#ifndef QUICKJS_ANDROID_JAVA_EXCEPTION_H
#define QUICKJS_ANDROID_JAVA_EXCEPTION_H

#include <jni.h>
#include <quickjs.h>
#include <stdbool.h>

#include "common.h"

#define CLASS_NAME_ILLEGAL_ARGUMENT_EXCEPTION "java/lang/IllegalArgumentException"
#define CLASS_NAME_ILLEGAL_STATE_EXCEPTION    "java/lang/IllegalStateException"
#define CLASS_NAME_OUT_OF_MEMORY_ERROR        "java/lang/OutOfMemoryError"
#define CLASS_NAME_JS_DATA_EXCEPTION          "com/hippo/quickjs/android/JSDataException"

#define THROW_EXCEPTION_RET(ENV, EXCEPTION_NAME, ...)                           \
    do {                                                                        \
        throw_exception((ENV), (EXCEPTION_NAME), __VA_ARGS__);                  \
        return;                                                                 \
    } while (0)

#define THROW_EXCEPTION_RET_STH(ENV, STH, EXCEPTION_NAME, ...)                  \
    do {                                                                        \
        throw_exception((ENV), (EXCEPTION_NAME), __VA_ARGS__);                  \
        return (STH);                                                           \
    } while (0)

#define THROW_JS_EVALUATION_EXCEPTION_RET(ENV, CTX)                             \
    do {                                                                        \
        throw_JSEvaluationException((ENV), (CTX));                              \
        return;                                                                 \
    } while (0)

#define THROW_ILLEGAL_ARGUMENT_EXCEPTION_RET(ENV, ...)                          \
    THROW_EXCEPTION_RET(ENV, CLASS_NAME_ILLEGAL_ARGUMENT_EXCEPTION, __VA_ARGS__)

#define THROW_OUT_OF_MEMORY_ERROR_RET(ENV)                                      \
    THROW_EXCEPTION_RET(ENV, CLASS_NAME_OUT_OF_MEMORY_ERROR, "")

#define THROW_OUT_OF_MEMORY_ERROR_RET_STH(ENV, STH)                             \
    THROW_EXCEPTION_RET_STH(ENV, STH, CLASS_NAME_OUT_OF_MEMORY_ERROR, "")

#define CHECK_NULL_IA_RET(ENV, PTR, ...)                                        \
    do {                                                                        \
        if (unlikely((PTR) == NULL)) {                                          \
            THROW_ILLEGAL_ARGUMENT_EXCEPTION_RET((ENV), __VA_ARGS__);           \
        }                                                                       \
    } while (0)

#define CHECK_NULL_OOM_RET(ENV, PTR)                                            \
    do {                                                                        \
        if (unlikely((PTR) == NULL)) {                                          \
            THROW_OUT_OF_MEMORY_ERROR_RET((ENV));                               \
        }                                                                       \
    } while (0)

#define CHECK_NULL_OOM_RET_STH(ENV, PTR, STH)                                   \
    do {                                                                        \
        if (unlikely((PTR) == NULL)) {                                          \
            THROW_OUT_OF_MEMORY_ERROR_RET_STH((ENV), (STH));                    \
        }                                                                       \
    } while (0)

void throw_exception(JNIEnv *env, const char *exception_name, const char *message, ...);

void throw_JSEvaluationException(JNIEnv *env, JSContext *ctx);

bool java_exception_init(JNIEnv *env);

#endif //QUICKJS_ANDROID_JAVA_EXCEPTION_H
