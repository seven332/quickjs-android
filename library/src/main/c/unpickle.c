#include <jni.h>

#include "unpickle.h"
#include "js-value-stack.h"

#define DEFAULT_STACK_SIZE 8

#define FLAG_PROP_INT              ((int8_t) 0b00000000)
#define FLAG_PROP_STR              ((int8_t) 0b00000001)

#define FLAG_TYPE_NULL             ((int8_t) 0b10000000)
#define FLAG_TYPE_BOOLEAN          ((int8_t) 0b10000001)
#define FLAG_TYPE_INT              ((int8_t) 0b10000010)
#define FLAG_TYPE_DOUBLE           ((int8_t) 0b10000011)
#define FLAG_TYPE_STRING           ((int8_t) 0b10000100)
#define FLAG_TYPE_OBJECT           ((int8_t) 0b10000101)
#define FLAG_TYPE_ARRAY            ((int8_t) 0b10000110)
#define FLAG_TYPE_COMMAND          ((int8_t) 0b10000111)

#define FLAG_ATTR_NULLABLE         ((int8_t) 0b01000000)

#define FLAG_OPT_PUSH              ((int8_t) 0b11000000)
#define FLAG_OPT_POP               ((int8_t) 0b11000001)

static JSValue do_unpickle(JSContext *ctx, JSValueStack *stack, BitSource *command, BitSource *source) {
    while (true) {
        JSValue val;
        int flag = bit_source_next_int8(command);

        if (flag == FLAG_OPT_PUSH) {
            val = JS_NewObject(ctx);
            js_value_stack_push(stack, val);
            break;
        }

        bool skipped = false;

        if (flag == FLAG_ATTR_NULLABLE) {
            int32_t segment_size = bit_source_next_int32(command);
            bool is_non_null = bit_source_next_int8(source);
            if (is_non_null) {
                flag = bit_source_next_int8(command);
            } else {
                bit_source_skip(command, (size_t) segment_size);
                val = JS_NULL;
                skipped = true;
            }
        }

        if (!skipped) {
            switch (flag) {
                case FLAG_TYPE_NULL:
                    val = JS_NULL;
                    break;
                case FLAG_TYPE_BOOLEAN:
                    val = JS_NewBool(ctx, bit_source_next_int8(source));
                    break;
                case FLAG_TYPE_INT:
                    val = JS_NewInt32(ctx, bit_source_next_int32(source));
                    break;
                case FLAG_TYPE_DOUBLE:
                    val = JS_NewFloat64(ctx, bit_source_next_double(source));
                    break;
                case FLAG_TYPE_STRING:
                    val = JS_NewString(ctx, bit_source_next_string(source));
                    break;
                case FLAG_TYPE_ARRAY: {
                    val = JS_NewArray(ctx);
                    uint32_t len = (uint32_t) bit_source_next_int32(source);
                    size_t segment_size = (size_t) bit_source_next_int32(command);
                    size_t segment_offset = bit_source_get_offset(command);
                    size_t command_size = bit_source_get_size(command);
                    for (uint32_t i = 0; i < len; i++) {
                        bit_source_reconfig(command, segment_offset, segment_offset + segment_size);
                        size_t start = js_value_stack_mark(stack);

                        // TODO Check JS_EXCEPTION
                        JSValue element = do_unpickle(ctx, stack, command, source);
                        JS_SetPropertyUint32(ctx, val, (uint32_t) i, element);

                        js_value_stack_reset(stack, start);
                    }
                    bit_source_reconfig(command, segment_offset + segment_size, command_size);
                    break;
                }
                case FLAG_TYPE_COMMAND: {
                    void *child = (void *) bit_source_next_int64(command);
                    BitSource child_command = CREATE_COMMAND_BIT_SOURCE(child);
                    size_t start = js_value_stack_mark(stack);
                    // TODO Check JS_EXCEPTION
                    val = do_unpickle(ctx, stack, &child_command, source);
                    js_value_stack_reset(stack, start);
                    break;
                }
                case FLAG_OPT_POP: {
                    if (js_value_stack_is_empty(stack)) goto fail;
                    val = js_value_stack_pop(stack);
                    break;
                }
                default:
                    goto fail;

            }
        }

        // No parent, it must be the result
        if (js_value_stack_is_empty(stack)) {
            // Command must be consumed
            assert(!bit_source_has_next(command));
            return val;
        }

        flag = bit_source_next_int8(command);
        switch (flag) {
            case FLAG_PROP_INT: {
                uint32_t index = (uint32_t) bit_source_next_int32(command);
                JSValue parent = js_value_stack_peek(stack);
                JS_SetPropertyUint32(ctx, parent, index, val);
                break;
            }
            case FLAG_PROP_STR: {
                const char *name = bit_source_next_string(command);
                JSValue parent = js_value_stack_peek(stack);
                JS_SetPropertyStr(ctx, parent, name, val);
                break;
            }
            default:
                goto fail;
        }
    }

fail:
    // TODO free stack
    return JS_EXCEPTION;
}

JSValue unpickle(JSContext *ctx, BitSource *command, BitSource *source) {
    JSValueStack stack;
    if (!create_js_value_stack(&stack, DEFAULT_STACK_SIZE)) return false;
    JSValue result = do_unpickle(ctx, &stack, command, source);
    // Source must be consumed
    assert(!bit_source_has_next(source));
    destroy_js_value_stack(&stack, ctx);
    return result;
}
