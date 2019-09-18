#ifndef QUICKJS_ANDROID_COMMON_H
#define QUICKJS_ANDROID_COMMON_H

#define force_inline inline __attribute__((always_inline))
#define likely(x)    __builtin_expect(!!(x), 1)
#define unlikely(x)  __builtin_expect(!!(x), 0)

#define EMPTY_STRING ""

#define ERROR_TYPE_OOM              0
#define ERROR_TYPE_JS_DATA          1
#define ERROR_TYPE_JS_EVALUATION    2
#define ERROR_TYPE_ILLEGAL_ARGUMENT 3
#define ERROR_TYPE_ILLEGAL_STATE    4

#endif //QUICKJS_ANDROID_COMMON_H
