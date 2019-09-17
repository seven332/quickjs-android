#ifndef QUICKJS_ANDROID_COMMON_H
#define QUICKJS_ANDROID_COMMON_H

#define force_inline inline __attribute__((always_inline))
#define likely(x)    __builtin_expect(!!(x), 1)
#define unlikely(x)  __builtin_expect(!!(x), 0)

#endif //QUICKJS_ANDROID_COMMON_H
