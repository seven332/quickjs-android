#include <assert.h>
#include <malloc.h>
#include <jni.h>

#include "pickle.h"
#include "js-value-stack.h"

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

// Returns -1 if fail
static int JS_GetArrayLength(JSContext *ctx, JSValue val) {
    if (!JS_IsArray(ctx, val)) return -1;
    JSValue length = JS_GetPropertyStr(ctx, val, "length");
    if (JS_VALUE_GET_NORM_TAG(length) != JS_TAG_INT) return -1;
    return JS_VALUE_GET_INT(length);
}

bool do_pickle(JSContext *ctx, JSValue val, JSValueStack* stack, BitSource *command, BitSink *sink) {
    JSValue callee;
    bool is_prop;

    while (bit_source_has_next(command)) {
        int8_t flag = bit_source_next_int8(command);

        if (flag == FLAG_OPT_POP) {
            JS_FreeValue(ctx, val);
            val = js_value_stack_pop(stack);
            continue;
        }

        switch (flag) {
            case FLAG_PROP_INT:
                callee = JS_GetPropertyUint32(ctx, val, (uint32_t) bit_source_next_int32(command));
                flag = bit_source_next_int8(command);
                is_prop = true;
                break;
            case FLAG_PROP_STR:
                callee = JS_GetPropertyStr(ctx, val, bit_source_next_string(command));
                flag = bit_source_next_int8(command);
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
            int32_t segment_size = bit_source_next_int32(command);
            if (tag == JS_TAG_NULL || tag == JS_TAG_UNDEFINED) {
                if (!bit_sink_write_null(sink)) goto fail;
                bit_source_skip(command, (size_t) segment_size);
                skipped = true;
            } else {
                flag = bit_source_next_int8(command);
            }
        }

        if (!skipped) {
            switch (flag) {
                case FLAG_OPT_PUSH:
                    assert(is_prop);
                    js_value_stack_push(stack, val);
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

                    size_t segment_size = (size_t) bit_source_next_int32(command);
                    size_t segment_offset = bit_source_get_offset(command);
                    size_t command_size = bit_source_get_size(command);
                    for (int32_t i = 0; i < len; i++) {
                        bit_source_reconfig(command, segment_offset, segment_offset + segment_size);
                        JSValue element = JS_GetPropertyUint32(ctx, callee, (uint32_t) i);
                        if (JS_IsException(element)) goto fail;

                        size_t start = js_value_stack_mark(stack);
                        bool pickled = do_pickle(ctx, element, stack, command, sink);
                        js_value_stack_reset(stack, start);

                        JS_FreeValue(ctx, element);
                        if (!pickled) goto fail;
                    }
                    bit_source_reconfig(command, segment_offset + segment_size, command_size);
                    break;
                }
                case FLAG_TYPE_COMMAND: {
                    void *child = (void *) bit_source_next_int64(command);
                    BitSource child_source = CREATE_COMMAND_BIT_SOURCE(child);
                    size_t start = js_value_stack_mark(stack);
                    bool pickled = do_pickle(ctx, callee, stack, &child_source, sink);
                    js_value_stack_reset(stack, start);
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

    js_value_stack_clear(stack, ctx);
    return true;

fail:
    if (is_prop) {
        JS_FreeValue(ctx, callee);
    }
    if (!js_value_stack_is_empty(stack)) {
        JS_FreeValue(ctx, val);
    }
    js_value_stack_clear(stack, ctx);
    return false;
}

bool pickle(JSContext *ctx, JSValue val, BitSource *source, BitSink *sink) {
    JSValueStack stack;
    if (!create_js_value_stack(&stack, DEFAULT_STACK_SIZE)) return false;
    bool result = do_pickle(ctx, val, &stack, source, sink);
    destroy_js_value_stack(&stack, ctx);
    return result;
}
