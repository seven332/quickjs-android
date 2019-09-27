#ifndef QUICKJS_ANDROID_JS_VALUE_STACK_H
#define QUICKJS_ANDROID_JS_VALUE_STACK_H

#include <quickjs.h>
#include <assert.h>
#include <stdbool.h>
#include <malloc.h>
#include <limits.h>

#include "common.h"

typedef struct JSValueStack {
    JSValue *data;
    size_t start;
    size_t offset;
    size_t size;
} JSValueStack;

static force_inline bool create_js_value_stack(JSValueStack *stack, size_t size) {
    stack->data = (JSValue *) malloc(size * sizeof(JSValue));
    if (unlikely(stack->data == NULL)) return false;

    stack->start = 0;
    stack->offset = 0;
    stack->size = size;

    return true;
}

static force_inline JSValue js_value_stack_pop(JSValueStack *stack) {
    assert(stack->offset > stack->start);
    return stack->data[--stack->offset];
}

static force_inline JSValue js_value_stack_peek(JSValueStack *stack) {
    assert(stack->offset > stack->start);
    return stack->data[stack->offset - 1];
}

static force_inline bool js_value_stack_push(JSValueStack *stack, JSValue val) {
    size_t min_size = stack->offset + 1;
    // Check overflow
    if (unlikely(min_size < stack->offset)) return false;

    if (min_size > stack->size) {
        size_t new_size = stack->size * 2;
        // Check overflow
        if (unlikely(new_size < stack->size)) new_size = SIZE_T_MAX;

        if (min_size > new_size) new_size = min_size;

        JSValue *new_data = (JSValue *) realloc(stack->data, new_size * sizeof(JSValue));
        if (unlikely(new_data == NULL)) return false;

        stack->data = new_data;
        stack->size = new_size;
    }

    stack->data[stack->offset++] = val;
    return true;
}

static force_inline size_t js_value_stack_mark(JSValueStack *stack) {
    size_t start = stack->start;
    stack->start = stack->offset;
    return start;
}

static force_inline void js_value_stack_reset(JSValueStack *stack, size_t start) {
    stack->start = start;
}

static force_inline bool js_value_stack_is_empty(JSValueStack *stack) {
    return stack->start == stack->offset;
}

static force_inline void js_value_stack_clear(JSValueStack *stack, JSContext *ctx) {
    for (size_t i = stack->start; i < stack->offset; i++) {
        JS_FreeValue(ctx, stack->data[i]);
    }
    stack->offset = stack->start;
}

static force_inline void destroy_js_value_stack(JSValueStack *stack, JSContext *ctx) {
    assert(stack->start == 0);
    assert(stack->offset == 0);
    free(stack->data);
}

#endif //QUICKJS_ANDROID_JS_VALUE_STACK_H
