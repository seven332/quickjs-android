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

#define GO_TO_FAIL_OOM        \
do {                          \
    JS_ThrowOutOfMemory(ctx); \
    goto fail;                \
} while (0)

#define GO_TO_FAIL_UNEXPECTED_TAG                                                     \
do {                                                                                  \
    JS_ThrowInternalError(ctx, "Unexpected js tag %d for pickle flag %d", tag, flag); \
    goto fail;                                                                        \
} while (0)

bool do_pickle(JSContext *ctx, JSValue val, JSValueStack* stack, BitSource *command, BitSink *sink) {
    bool freed;
    while (true) {
        freed = false;
        int8_t flag = bit_source_next_int8(command);

        if (flag == FLAG_OPT_POP) {
            val = js_value_stack_pop(stack);
            JS_FreeValue(ctx, val);
            // No more JSValue, that's all
            if (js_value_stack_is_empty(stack)) {
                // Command must be consumed
                assert(!bit_source_has_next(command));
                return true;
            } else {
                continue;
            }
        }

        if (!js_value_stack_is_empty(stack)) {
            val = js_value_stack_peek(stack);
            if (likely(flag == FLAG_PROP_STR)) {
                const char *name = bit_source_next_string(command);
                val = JS_GetPropertyStr(ctx, val, name);
            } else if (flag == FLAG_PROP_INT) {
                uint32_t index = (uint32_t) bit_source_next_int32(command);
                val = JS_GetPropertyUint32(ctx, val, index);
            } else {
                assert(false && "Unexpected pickle flag");
            }
            if (JS_IsException(val)) goto fail;
            flag = bit_source_next_int8(command);
        }

        int tag = JS_VALUE_GET_NORM_TAG(val);
        bool skipped = false;

        if (flag == FLAG_ATTR_NULLABLE) {
            int32_t segment_size = bit_source_next_int32(command);
            if (tag == JS_TAG_NULL || tag == JS_TAG_UNDEFINED) {
                if (unlikely(!bit_sink_write_boolean(sink, false))) GO_TO_FAIL_OOM;
                bit_source_skip(command, (size_t) segment_size);
                skipped = true;
            } else {
                if (unlikely(!bit_sink_write_boolean(sink, true))) GO_TO_FAIL_OOM;
                flag = bit_source_next_int8(command);
            }
        }

        if (!skipped) {
            switch (flag) {
                case FLAG_TYPE_NULL:
                    if (unlikely(tag != JS_TAG_NULL && tag != JS_TAG_UNDEFINED)) GO_TO_FAIL_UNEXPECTED_TAG;
                    // Write nothing to sink
                    break;
                case FLAG_TYPE_BOOLEAN:
                    if (unlikely(tag != JS_TAG_BOOL)) GO_TO_FAIL_UNEXPECTED_TAG;
                    if (unlikely(!bit_sink_write_boolean(sink, (int8_t) JS_VALUE_GET_BOOL(val)))) GO_TO_FAIL_OOM;
                    break;
                case FLAG_TYPE_NUMBER:
                    switch (tag) {
                        case JS_TAG_INT:
                            if (unlikely(!bit_sink_write_number_int(sink, (int32_t) JS_VALUE_GET_INT(val)))) GO_TO_FAIL_OOM;
                            break;
                        case JS_TAG_FLOAT64:
                            if (unlikely(!bit_sink_write_number_double(sink, JS_VALUE_GET_FLOAT64(val)))) GO_TO_FAIL_OOM;
                            break;
                        default:
                            GO_TO_FAIL_UNEXPECTED_TAG;
                    }
                    break;
                case FLAG_TYPE_STRING:
                    if (unlikely(tag != JS_TAG_STRING)) GO_TO_FAIL_UNEXPECTED_TAG;
                    const char *str = JS_ToCString(ctx, val);
                    if (unlikely(str == NULL)) GO_TO_FAIL_OOM;
                    bool wrote = bit_sink_write_string(sink, str);
                    JS_FreeCString(ctx, str);
                    if (unlikely(!wrote)) GO_TO_FAIL_OOM;
                    break;
                case FLAG_TYPE_ARRAY: {
                    int32_t len = JS_GetArrayLength(ctx, val);
                    if (unlikely(len < 0)) GO_TO_FAIL_UNEXPECTED_TAG;
                    if (unlikely(!bit_sink_write_array_length(sink, len))) GO_TO_FAIL_OOM;

                    size_t segment_size = (size_t) bit_source_next_int32(command);
                    size_t segment_offset = bit_source_get_offset(command);
                    size_t command_size = bit_source_get_size(command);
                    for (int32_t i = 0; i < len; i++) {
                        bit_source_reconfig(command, segment_offset, segment_offset + segment_size);
                        JSValue element = JS_GetPropertyUint32(ctx, val, (uint32_t) i);
                        if (JS_IsException(element)) goto fail;

                        size_t start = js_value_stack_mark(stack);
                        bool pickled = do_pickle(ctx, element, stack, command, sink);
                        js_value_stack_reset(stack, start);

                        // No need to reconfig command if "goto fail"
                        if (unlikely(!pickled)) goto fail;
                    }
                    bit_source_reconfig(command, segment_offset + segment_size, command_size);
                    break;
                }
                case FLAG_TYPE_COMMAND: {
                    void *child = (void *) bit_source_next_int64(command);
                    BitSource child_command = CREATE_COMMAND_BIT_SOURCE(child);

                    size_t start = js_value_stack_mark(stack);
                    bool pickled = do_pickle(ctx, val, stack, &child_command, sink);
                    freed = true;
                    js_value_stack_reset(stack, start);

                    if (unlikely(!pickled)) goto fail;
                    break;
                }
                case FLAG_OPT_PUSH: {
                    if (unlikely(tag != JS_TAG_OBJECT)) GO_TO_FAIL_UNEXPECTED_TAG;
                    if (unlikely(!js_value_stack_push(stack, val))) GO_TO_FAIL_OOM;
                    // Start a new turn
                    continue;
                }
                default:
                    assert(false && "Unexpected pickle flag");
                    break;
            }
        }

        if (!freed) JS_FreeValue(ctx, val);

        // No more JSValue, that's all
        // It's for non-object JSValue
        if (js_value_stack_is_empty(stack)) {
            // Command must be consumed
            assert(!bit_source_has_next(command));
            return true;
        }
    }

fail:
    if (!freed) JS_FreeValue(ctx, val);
    js_value_stack_clear(stack, ctx);
    return false;
}

bool pickle(JSContext *ctx, JSValue val, BitSource *source, BitSink *sink) {
    JSValueStack stack;
    if (unlikely(!create_js_value_stack(&stack, DEFAULT_STACK_SIZE))) return false;
    bool result = do_pickle(ctx, val, &stack, source, sink);
    destroy_js_value_stack(&stack, ctx);
    return result;
}
