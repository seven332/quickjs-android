#include <jni.h>
#include <quickjs.h>

#include "quick-java.h"

// TODO append the java exception to the js exception
#define CHECK_JAVA_EXCEPTION_NO(ENV)      \
    if ((*(ENV))->ExceptionCheck(ENV)) {  \
        (*(ENV))->ExceptionDescribe(ENV); \
        (*(ENV))->ExceptionClear(ENV);    \
        return -1;                        \
    }

// TODO append the java exception to the js exception
#define CHECK_JAVA_EXCEPTION_NULL(ENV)    \
    if ((*(ENV))->ExceptionCheck(ENV)) {  \
        (*(ENV))->ExceptionDescribe(ENV); \
        (*(ENV))->ExceptionClear(ENV);    \
        return NULL;                      \
    }

// TODO append the java exception to the js exception
#define CHECK_JAVA_EXCEPTION_JS_EXCEPTION(CTX, ENV)  \
    if ((*(ENV))->ExceptionCheck(ENV)) {             \
        (*(ENV))->ExceptionDescribe(ENV);            \
        (*(ENV))->ExceptionClear(ENV);               \
        return JS_ThrowInternalError((CTX), "Catch java exception"); \
    }

// typedef JSValue JavaStaticMethodCaller(JSContext *ctx, JNIEnv *env, jobject js_context, jobject return_type, jclass clazz, jmethodID method, jvalue *argv);

#define OBTAIN_ENV(VM)                                                                              \
    JNIEnv *env = NULL;                                                                             \
    int __require_detach__ = 0;                                                                     \
    (*(VM))->GetEnv((VM), (void **) &env, JNI_VERSION_1_6);                                         \
    if (env == NULL) __require_detach__ = (*(VM))->AttachCurrentThread((VM), &env, NULL) == JNI_OK;

#define RELEASE_ENV(VM)                                           \
    if (__require_detach__) (*(VM))->DetachCurrentThread((VM));

static int js_value_to_java_value(JSContext *ctx, JNIEnv *env, jobject js_context, jobject type, JSValueConst value, jvalue *result);

static JSClassID java_method_class_id;

typedef JSValue (*JavaMethodCaller)(JSContext *ctx, JNIEnv *env, jobject js_context, jobject return_type, jobject instance, jmethodID method, jvalue *argv);

typedef struct {
    JavaVM *vm;
    jobject js_context;
    jobject instance;
    jmethodID method;
    jobject return_type;
    int arg_count;
    jobject *arg_types;
    JavaMethodCaller caller;
} JavaMethodData;

static JSValue java_method_call(JSContext *ctx, JSValueConst func_obj, JSValueConst this_val, int argc, JSValueConst *argv) {
    JavaMethodData *data = JS_GetOpaque(func_obj, java_method_class_id);

    if (argc != data->arg_count) {
        // TODO it's not internal, it blames on the caller
        return JS_ThrowInternalError(ctx, "Inconsistent argument count, excepted: %d, actual: %d", data->arg_count, argc);
    }

    OBTAIN_ENV(data->vm);

    // Convert js value arguments to java value arguments
    jvalue java_argv[argc];
    for (int i = 0; i < argc; i++) {
        if (js_value_to_java_value(ctx, env, data->js_context, data->arg_types[i], argv[i], java_argv + i)) {
            goto fail;
        }
    }

    JSValue result = data->caller(ctx, env, data->js_context, data->return_type, data->instance, data->method, java_argv);

    RELEASE_ENV(data->vm);
    return result;

fail:
    RELEASE_ENV(data->vm);
    return JS_ThrowInternalError(ctx, "Failed to convert js value to java value");
}

static void java_method_finalizer(JSRuntime *rt, JSValue val) {
    JavaMethodData *data = JS_GetOpaque(val, java_method_class_id);

    OBTAIN_ENV(data->vm);

    if (env != NULL) {
        (*env)->DeleteGlobalRef(env, data->instance);
        (*env)->DeleteGlobalRef(env, data->js_context);
        (*env)->DeleteGlobalRef(env, data->return_type);
        for (int i = 0; i < data->arg_count; i++) {
            (*env)->DeleteGlobalRef(env, data->arg_types[i]);
        }
    }

    RELEASE_ENV(data->vm);

    js_free_rt(rt, data->arg_types);
    js_free_rt(rt, data);
}

static JSClassDef java_method_class = {
        "JavaMethod",
        .call = java_method_call,
        .finalizer = java_method_finalizer
};

int quick_java_init_java(JSContext *ctx) {
    JS_NewClassID(&java_method_class_id);
    if (JS_NewClass(JS_GetRuntime(ctx), java_method_class_id, &java_method_class)) return -1;
    return 0;
}

jclass jni_helper_class;
jmethodID js_value_to_java_value_method;
jmethodID java_boolean_to_js_value_method;
jmethodID java_char_to_js_value_method;
jmethodID java_byte_to_js_value_method;
jmethodID java_short_to_js_value_method;
jmethodID java_int_to_js_value_method;
jmethodID java_long_to_js_value_method;
jmethodID java_float_to_js_value_method;
jmethodID java_double_to_js_value_method;
jmethodID java_object_to_js_value_method;
jmethodID is_primitive_type_method;
jmethodID is_same_type_method;
jmethodID unbox_boolean_method;
jmethodID unbox_char_method;
jmethodID unbox_byte_method;
jmethodID unbox_short_method;
jmethodID unbox_int_method;
jmethodID unbox_long_method;
jmethodID unbox_float_method;
jmethodID unbox_double_method;
jobject void_primitive_type;
jobject char_primitive_type;
jobject boolean_primitive_type;
jobject byte_primitive_type;
jobject short_primitive_type;
jobject int_primitive_type;
jobject long_primitive_type;
jobject float_primitive_type;
jobject double_primitive_type;

int quick_java_init(JNIEnv *env) {
    jni_helper_class = (*env)->FindClass(env, "com/hippo/quickjs/android/JNIHelper");
    jni_helper_class = (*env)->NewGlobalRef(env, jni_helper_class);
    if (jni_helper_class == NULL) return -1;

#define GET_STATIC_METHOD(RESULT, NAME, SIGN)                                    \
    (RESULT) = (*env)->GetStaticMethodID(env, jni_helper_class, (NAME), (SIGN)); \
    if ((RESULT) == NULL) return -1;

    GET_STATIC_METHOD(js_value_to_java_value_method, "jsValueToJavaValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;J)Ljava/lang/Object;");
    GET_STATIC_METHOD(java_boolean_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;Z)J");
    GET_STATIC_METHOD(java_char_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;C)J");
    GET_STATIC_METHOD(java_byte_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;B)J");
    GET_STATIC_METHOD(java_short_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;S)J");
    GET_STATIC_METHOD(java_int_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;I)J");
    GET_STATIC_METHOD(java_long_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;J)J");
    GET_STATIC_METHOD(java_float_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;F)J");
    GET_STATIC_METHOD(java_double_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;D)J");
    GET_STATIC_METHOD(java_object_to_js_value_method, "javaValueToJSValue", "(Lcom/hippo/quickjs/android/JSContext;Ljava/lang/reflect/Type;Ljava/lang/Object;)J");
    GET_STATIC_METHOD(is_primitive_type_method, "isPrimitiveType", "(Ljava/lang/reflect/Type;)Z");
    GET_STATIC_METHOD(is_same_type_method, "isSameType", "(Ljava/lang/reflect/Type;Ljava/lang/reflect/Type;)Z");
    GET_STATIC_METHOD(unbox_boolean_method, "unbox", "(Ljava/lang/Boolean;)Z");
    GET_STATIC_METHOD(unbox_char_method, "unbox", "(Ljava/lang/Character;)C");
    GET_STATIC_METHOD(unbox_byte_method, "unbox", "(Ljava/lang/Byte;)B");
    GET_STATIC_METHOD(unbox_short_method, "unbox", "(Ljava/lang/Short;)S");
    GET_STATIC_METHOD(unbox_int_method, "unbox", "(Ljava/lang/Integer;)I");
    GET_STATIC_METHOD(unbox_long_method, "unbox", "(Ljava/lang/Long;)J");
    GET_STATIC_METHOD(unbox_float_method, "unbox", "(Ljava/lang/Float;)F");
    GET_STATIC_METHOD(unbox_double_method, "unbox", "(Ljava/lang/Double;)D");

#undef GET_STATIC_METHOD

    jfieldID field_id;
#define GET_PRIMITIVE_TYPE(RESULT, NAME)                                                            \
    field_id = (*env)->GetStaticFieldID(env, jni_helper_class, (NAME), "Ljava/lang/reflect/Type;"); \
    if (field_id == NULL) return -1;                                                                \
    (RESULT) = (*env)->GetStaticObjectField(env, jni_helper_class, field_id);                       \
    (RESULT) = (*env)->NewGlobalRef(env, (RESULT));                                                 \
    if ((RESULT) == NULL) return -1;

    GET_PRIMITIVE_TYPE(void_primitive_type, "VOID_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(char_primitive_type, "CHAR_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(boolean_primitive_type, "BOOLEAN_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(byte_primitive_type, "BYTE_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(short_primitive_type, "SHORT_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(int_primitive_type, "INT_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(long_primitive_type, "LONG_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(float_primitive_type, "FLOAT_PRIMITIVE_TYPE");
    GET_PRIMITIVE_TYPE(double_primitive_type, "DOUBLE_PRIMITIVE_TYPE");

#undef GET_PRIMITIVE_TYPE

    return 0;
}

static int unbox_primitive_type(JNIEnv *env, jobject type, jvalue *value) {
    jboolean is_primitive_type = (*env)->CallStaticBooleanMethod(env, jni_helper_class, is_primitive_type_method, type);
    CHECK_JAVA_EXCEPTION_NO(env);
    if (!is_primitive_type) return 0;

    jboolean is_the_type;
#define UNBOX_PRIMITIVE_TYPE(TYPE, GETTER, CALLER, TARGET) \
    is_the_type = (*env)->CallStaticBooleanMethod(env, jni_helper_class, is_same_type_method, type, (TYPE)); \
    CHECK_JAVA_EXCEPTION_NO(env); \
    if (is_the_type) { \
        (TARGET) = (*env)->CALLER(env, jni_helper_class, GETTER, value->l);\
        CHECK_JAVA_EXCEPTION_NO(env); \
        return 0; \
    }

    UNBOX_PRIMITIVE_TYPE(boolean_primitive_type, unbox_boolean_method, CallStaticBooleanMethod, value->z);
    UNBOX_PRIMITIVE_TYPE(char_primitive_type, unbox_char_method, CallStaticCharMethod, value->c);
    UNBOX_PRIMITIVE_TYPE(byte_primitive_type, unbox_byte_method, CallStaticByteMethod, value->b);
    UNBOX_PRIMITIVE_TYPE(short_primitive_type, unbox_short_method, CallStaticShortMethod, value->s);
    UNBOX_PRIMITIVE_TYPE(int_primitive_type, unbox_int_method, CallStaticIntMethod, value->i);
    UNBOX_PRIMITIVE_TYPE(long_primitive_type, unbox_long_method, CallStaticLongMethod, value->j);
    UNBOX_PRIMITIVE_TYPE(float_primitive_type, unbox_float_method, CallStaticFloatMethod, value->f);
    UNBOX_PRIMITIVE_TYPE(double_primitive_type, unbox_double_method, CallStaticDoubleMethod, value->d);

#undef UNBOX_PRIMITIVE_TYPE

    // TODO Unknown primitive type
    return -1;
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

static int js_value_to_java_value(
        JSContext *ctx,
        JNIEnv *env,
        jobject js_context,
        jobject type,
        JSValueConst value,
        jvalue *result
) {
    JSValue *copy = NULL;
    // Duplication is required
    JS_DupValue(ctx, value);
    COPY_JS_VALUE(ctx, value, copy);
    if (copy == NULL) return -1;

    result->l = (*env)->CallStaticObjectMethod(env, jni_helper_class, js_value_to_java_value_method, js_context, type, (jlong) copy);
    CHECK_JAVA_EXCEPTION_NO(env);

    return unbox_primitive_type(env, type, result);
}

#define FUNCTION_CALL_JAVA_METHOD(FUNCTION_NAME, JAVA_TYPE, JAVA_CALLER, JAVA_CONVERTER)                                                               \
static JSValue FUNCTION_NAME(JSContext *ctx, JNIEnv *env, jobject js_context, jobject return_type, jobject instance, jmethodID method, jvalue *argv) { \
    JAVA_TYPE java_result = (*env)->JAVA_CALLER(env, instance, method, argv);                                                                          \
    CHECK_JAVA_EXCEPTION_JS_EXCEPTION(ctx, env);                                                                                                       \
    JSValue *result = (JSValue *) (*env)->CallStaticLongMethod(env, jni_helper_class, JAVA_CONVERTER, js_context, return_type, java_result);           \
    CHECK_JAVA_EXCEPTION_JS_EXCEPTION(ctx, env);                                                                                                       \
    return JS_DupValue(ctx, *result);                                                                                                                  \
}

static JSValue call_void_java_method(JSContext *ctx, JNIEnv *env, jobject js_context, jobject return_type, jobject instance, jmethodID method, jvalue *argv) {
    (*env)->CallVoidMethodA(env, instance, method, argv);
    CHECK_JAVA_EXCEPTION_JS_EXCEPTION(ctx, env);
    return JS_UNDEFINED;
}

FUNCTION_CALL_JAVA_METHOD(call_boolean_java_method, jboolean, CallBooleanMethodA, java_boolean_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_char_java_method, jchar, CallCharMethodA, java_char_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_byte_java_method, jbyte, CallByteMethodA, java_byte_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_short_java_method, jshort, CallShortMethodA, java_short_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_int_java_method, jint, CallIntMethodA, java_int_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_long_java_method, jlong, CallLongMethodA, java_long_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_float_java_method, jfloat, CallFloatMethodA, java_float_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_double_java_method, jdouble, CallDoubleMethodA, java_double_to_js_value_method)
FUNCTION_CALL_JAVA_METHOD(call_object_java_method, jobject, CallObjectMethodA, java_object_to_js_value_method)

static JavaMethodCaller select_java_method_caller(JNIEnv *env, jobject type) {
    jboolean is_primitive_type = (*env)->CallStaticBooleanMethod(env, jni_helper_class, is_primitive_type_method, type);
    CHECK_JAVA_EXCEPTION_NULL(env);
    if (!is_primitive_type) return call_object_java_method;

    jboolean is_the_type;
#define CHECK_PRIMITIVE_TYPE(TYPE, RESULT)                                                                   \
    is_the_type = (*env)->CallStaticBooleanMethod(env, jni_helper_class, is_same_type_method, type, (TYPE)); \
    CHECK_JAVA_EXCEPTION_NULL(env);                                                                          \
    if (is_the_type) return (RESULT);

    CHECK_PRIMITIVE_TYPE(void_primitive_type, call_void_java_method);
    CHECK_PRIMITIVE_TYPE(boolean_primitive_type, call_boolean_java_method);
    CHECK_PRIMITIVE_TYPE(char_primitive_type, call_char_java_method);
    CHECK_PRIMITIVE_TYPE(byte_primitive_type, call_byte_java_method);
    CHECK_PRIMITIVE_TYPE(short_primitive_type, call_short_java_method);
    CHECK_PRIMITIVE_TYPE(int_primitive_type, call_int_java_method);
    CHECK_PRIMITIVE_TYPE(long_primitive_type, call_long_java_method);
    CHECK_PRIMITIVE_TYPE(float_primitive_type, call_float_java_method);
    CHECK_PRIMITIVE_TYPE(double_primitive_type, call_double_java_method);

    return NULL;
}

JSValue QJ_NewJavaFunction(
        JSContext *ctx,
        JNIEnv *env,
        jobject js_context,
        jobject instance,
        jmethodID method,
        jobject return_type,
        int arg_count,
        jobject *arg_types
) {
    JavaMethodCaller caller = select_java_method_caller(env, return_type);
    if (caller == NULL) return JS_EXCEPTION;

    JSRuntime *rt = JS_GetRuntime(ctx);
    JavaMethodData *data = NULL;
    jobject *arg_types_copy = NULL;

    data = js_malloc_rt(rt, sizeof(JavaMethodData));
    if (data == NULL) goto oom;
    arg_types_copy = js_malloc_rt(rt, sizeof(jobject) * arg_count);
    if (arg_types_copy == NULL) goto oom;

    JSValue value = JS_NewObjectClass(ctx, java_method_class_id);
    if (JS_IsException(value)) {
        js_free_rt(rt, data);
        js_free_rt(rt, arg_types_copy);
        return value;
    }

    for (int i = 0; i < arg_count; i++) {
        arg_types_copy[i] = (*env)->NewGlobalRef(env, arg_types[i]);
    }

    (*env)->GetJavaVM(env, &data->vm);
    data->js_context = (*env)->NewGlobalRef(env, js_context);
    data->instance = (*env)->NewGlobalRef(env, instance);
    data->method = method;
    data->return_type = (*env)->NewGlobalRef(env, return_type);
    data->arg_count = arg_count;
    data->arg_types = arg_types_copy;
    data->caller = caller;

    JS_SetOpaque(value, data);

    return value;

oom:
    if (data != NULL) js_free_rt(rt, data);
    if (arg_types_copy != NULL) js_free_rt(rt, arg_types_copy);
    return JS_ThrowOutOfMemory(ctx);
}
