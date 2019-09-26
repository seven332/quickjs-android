#ifndef QUICKJS_ANDROID_JAVA_HELPER_H
#define QUICKJS_ANDROID_JAVA_HELPER_H

#include <jni.h>

#define OBTAIN_ENV(VM)                                                                              \
    JNIEnv *env = NULL;                                                                             \
    int __require_detach__ = 0;                                                                     \
    (*(VM))->GetEnv((VM), (void **) &env, JNI_VERSION_1_6);                                         \
    if (env == NULL) __require_detach__ = (*(VM))->AttachCurrentThread((VM), &env, NULL) == JNI_OK;

#define RELEASE_ENV(VM)                                           \
    if (__require_detach__) (*(VM))->DetachCurrentThread((VM));

#endif //QUICKJS_ANDROID_JAVA_HELPER_H
