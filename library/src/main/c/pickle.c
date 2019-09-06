#include <assert.h>
#include <malloc.h>
#include <jni.h>

#include "pickle.h"

#define DEFAULT_STACK_SIZE 8

#define FLAG_PROP_INT              ((int8_t) 0b00000000)
#define FLAG_PROP_STR              ((int8_t) 0b00000001)

#define FLAG_TYPE_NULL             ((int8_t) 0b10000000)
#define FLAG_TYPE_BOOLEAN          ((int8_t) 0b10000001)
#define FLAG_TYPE_NUMBER           ((int8_t) 0b10000010)
#define FLAG_TYPE_STRING           ((int8_t) 0b10000011)
#define FLAG_TYPE_OBJECT           ((int8_t) 0b10000100)
#define FLAG_TYPE_ARRAY            ((int8_t) 0b10000101)
#define FLAG_TYPE_COMMAND          ((int8_t) 0b10000110)

#define FLAG_ATTR_NULLABLE         ((int8_t) 0b01000000)

#define FLAG_OPT_PUSH              ((int8_t) 0b11000000)
#define FLAG_OPT_POP               ((int8_t) 0b11000001)

typedef struct Stack {
    JSValue *data;
    size_t offset;
    size_t size;
} Stack;

static Stack *create_stack(size_t size) {
    Stack *stack = malloc(sizeof(Stack));
    if (stack == NULL) {
        return NULL;
    }

    stack->data = malloc(size * sizeof(JSValue));
    if (stack->data == NULL) {
        free(stack);
        return NULL;
    }

    stack->offset = 0;
    stack->size = size;

    return stack;
}

static JSValue stack_pop(Stack *stack) {
    assert(stack->offset > 0);
    return stack->data[--stack->offset];
}

static bool stack_push(Stack *stack, JSValue val) {
    if (stack->offset + 1 > stack->size) {
        // TODO Overflow
        size_t new_size = (stack->offset + 1) << 2U;
        void *new_data = realloc(stack->data, new_size * sizeof(JSValue));
        if (new_data == NULL) {
            return false;
        }
        stack->data = new_data;
        stack->size = new_size;
    }

    stack->data[stack->offset++] = val;
    return true;
}

static void destroy_stack(Stack *stack, JSContext *ctx) {
    for (size_t i = 0; i < stack->offset; i++) {
        JS_FreeValue(ctx, stack->data[i]);
    }
    free(stack->data);
    free(stack);
}

// Returns -1 if fail
static int JS_GetArrayLength(JSContext *ctx, JSValue val) {
    if (!JS_IsArray(ctx, val)) return -1;
    JSValue length = JS_GetPropertyStr(ctx, val, "length");
    if (JS_VALUE_GET_NORM_TAG(length) != JS_TAG_INT) return -1;
    return JS_VALUE_GET_INT(length);
}

bool do_pickle(JSContext *ctx, JSValue val, Stack* stack, BitSource *source, BitSink *sink) {
    JSValue callee;
    bool is_prop;

    while (bit_source_has_next(source)) {
        int8_t flag = bit_source_next_int8(source);

        if (flag == FLAG_OPT_POP) {
            JS_FreeValue(ctx, val);
            val = stack_pop(stack);
            continue;
        }

        switch (flag) {
            case FLAG_PROP_INT:
                callee = JS_GetPropertyUint32(ctx, val, (uint32_t) bit_source_next_int32(source));
                flag = bit_source_next_int8(source);
                is_prop = true;
                break;
            case FLAG_PROP_STR:
                callee = JS_GetPropertyStr(ctx, val, bit_source_next_string(source));
                flag = bit_source_next_int8(source);
                is_prop = true;
                break;
            default:
                callee = val;
                is_prop = false;
                break;
        }

        int tag = JS_VALUE_GET_NORM_TAG(callee);
        bool pushed = false;
        bool skipped = false;

        if (flag == FLAG_ATTR_NULLABLE) {
            int32_t segment_size = bit_source_next_int32(source);
            if (tag == JS_TAG_NULL || tag == JS_TAG_UNDEFINED) {
                if (!bit_sink_write_null(sink)) goto fail;
                bit_source_skip(source, (size_t) segment_size);
                skipped = true;
            } else {
                flag = bit_source_next_int8(source);
            }
        }

        if (!skipped) {
            switch (flag) {
                case FLAG_OPT_PUSH:
                    assert(is_prop);
                    stack_push(stack, val);
                    val = callee;
                    pushed = true;
                    break;
                case FLAG_TYPE_NULL:
                    if (tag != JS_TAG_NULL && tag != JS_TAG_UNDEFINED) goto fail;
                    if (!bit_sink_write_null(sink)) goto fail;
                    break;
                case FLAG_TYPE_BOOLEAN:
                    if (tag != JS_TAG_BOOL) goto fail;
                    if (!bit_sink_write_boolean(sink, (int8_t) JS_VALUE_GET_BOOL(callee))) goto fail;
                    break;
                case FLAG_TYPE_NUMBER:
                    switch (tag) {
                        case JS_TAG_INT:
                            if (!bit_sink_write_int(sink, (int32_t) JS_VALUE_GET_INT(callee))) goto fail;
                            break;
                        case JS_TAG_FLOAT64:
                            if (!bit_sink_write_double(sink, JS_VALUE_GET_FLOAT64(callee))) goto fail;
                            break;
                        default:
                            goto fail;
                    }
                    break;
                case FLAG_TYPE_STRING:
                    if (tag != JS_TAG_STRING) goto fail;
                    const char *str = JS_ToCString(ctx, callee);
                    if (str == NULL) goto fail;
                    bool wrote = bit_sink_write_string(sink, str);
                    JS_FreeCString(ctx, str);
                    if (!wrote) goto fail;
                    break;
                case FLAG_TYPE_ARRAY: {
                    int32_t len = JS_GetArrayLength(ctx, callee);
                    if (len < 0) goto fail;
                    if (!bit_sink_write_int(sink, len)) goto fail;

                    size_t segment_size = (size_t) bit_source_next_int32(source);
                    size_t segment_offset = bit_source_get_offset(source);
                    size_t source_size = bit_source_get_size(source);
                    for (int32_t i = 0; i < len; i++) {
                        bit_source_reconfig(source, segment_offset, segment_offset + segment_size);
                        JSValue element = JS_GetPropertyUint32(ctx, callee, (uint32_t) i);
                        if (JS_IsException(element)) goto fail;
                        if (!do_pickle(ctx, element, stack, source, sink)) goto fail;
                    }
                    bit_source_reconfig(source, segment_offset + segment_size, source_size);
                    break;
                }
                case FLAG_TYPE_COMMAND: {
                    void *command = (void *) bit_source_next_int64(source);
                    BitSource *child_source = create_source_bit(command + sizeof(jsize),
                                                                (size_t) *(jsize *) command);
                    bool pickled = do_pickle(ctx, callee, stack, child_source, sink);
                    destroy_source(child_source);
                    if (!pickled) goto fail;
                    break;
                }
                default:
                    goto fail;
            }
        }

        if (is_prop && !pushed) {
            JS_FreeValue(ctx, callee);
        }
    }

    JS_FreeValue(ctx, val);
    return true;

fail:
    if (is_prop) {
        JS_FreeValue(ctx, callee);
    }
    JS_FreeValue(ctx, val);
    return false;
}

bool pickle(JSContext *ctx, JSValue val, BitSource *source, BitSink *sink) {
    Stack *stack = create_stack(DEFAULT_STACK_SIZE);

    bool result = do_pickle(ctx, val, stack, source, sink);

    if (result) {
        assert(stack->offset == 0);
    }
    destroy_stack(stack, ctx);

    return result;
}
