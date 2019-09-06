#ifndef QUICKJS_ANDROID_BIT_SINK_H
#define QUICKJS_ANDROID_BIT_SINK_H

#include <stdbool.h>
#include <string.h>

typedef struct BitSink BitSink;

BitSink *create_bit_sink(size_t size);

bool bit_sink_write_null(BitSink *sink);

bool bit_sink_write_boolean(BitSink *sink, int8_t value);

bool bit_sink_write_int(BitSink *sink, int32_t value);

bool bit_sink_write_double(BitSink *sink, double value);

bool bit_sink_write_string_len(BitSink *sink, const char *value, size_t length);

static inline bool bit_sink_write_string(BitSink *sink, const char *value) {
    return bit_sink_write_string_len(sink, value, strlen(value));
}

size_t bit_sink_get_length(BitSink *sink);

void *bit_sink_get_data(BitSink *sink);

void destroy_bit_sink(BitSink *sink);

#endif //QUICKJS_ANDROID_BIT_SINK_H
