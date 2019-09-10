#ifndef QUICKJS_ANDROID_BIT_SINK_H
#define QUICKJS_ANDROID_BIT_SINK_H

#include <stdbool.h>
#include <string.h>

#include "common.h"

typedef struct BitSink {
    void *data;
    size_t offset;
    size_t size;
} BitSink;

static force_inline bool create_bit_sink(BitSink *sink, size_t size) {
    sink->data = malloc(size);
    if (sink->data == NULL) return false;
    sink->offset = 0;
    sink->size = size;
    return true;
}

bool bit_sink_write_null(BitSink *sink);

bool bit_sink_write_boolean(BitSink *sink, int8_t value);

bool bit_sink_write_int(BitSink *sink, int32_t value);

bool bit_sink_write_double(BitSink *sink, double value);

bool bit_sink_write_string_len(BitSink *sink, const char *value, size_t length);

static force_inline bool bit_sink_write_string(BitSink *sink, const char *value) {
    return bit_sink_write_string_len(sink, value, strlen(value));
}

static force_inline size_t bit_sink_get_length(BitSink *sink) {
    return sink->offset;
}

static force_inline void *bit_sink_get_data(BitSink *sink) {
    return sink->data;
}

static force_inline void destroy_bit_sink(BitSink *sink) {
    free(sink->data);
}

#endif //QUICKJS_ANDROID_BIT_SINK_H
