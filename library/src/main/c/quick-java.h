#ifndef QUICKJS_ANDROID_QUICK_JAVA_H
#define QUICKJS_ANDROID_QUICK_JAVA_H

#include <string.h>

int quick_java_init(JNIEnv *env);

int quick_java_init_java(JSContext *ctx);

JSValue QJ_NewJavaFunction(JSContext *ctx, JNIEnv *env, jobject js_context, jboolean is_static, jobject callee, jmethodID method, jobject return_type, int arg_count, jobject *arg_types);

#endif //QUICKJS_ANDROID_QUICK_JAVA_H
