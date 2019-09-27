#include <malloc.h>
#include <string.h>
#include <limits.h>

#include "bit-sink.h"

#define TYPE_INT    0
#define TYPE_DOUBLE 1

static bool ensure_size(BitSink *sink, size_t size) {
    size_t min_size = sink->offset + size;
    // Check overflow
    if (unlikely(min_size < sink->offset)) return false;

    // Enough
    if (min_size <= sink->size) return true;

    size_t new_size = sink->size * 2;
    // Check overflow
    if (unlikely(new_size < sink->size)) new_size = SIZE_T_MAX;

    if (min_size > new_size) new_size = min_size;

    void *new_data = realloc(sink->data, new_size);
    if (unlikely(new_data == NULL)) return false;

    sink->data = new_data;
    sink->size = new_size;

    return true;
}

bool bit_sink_write_boolean(BitSink *sink, int8_t value) {
    if (unlikely(!ensure_size(sink, 1))) return false;
    *((int8_t *) (sink->data + sink->offset)) = value;
    sink->offset += 1;
    return true;
}

bool bit_sink_write_array_length(BitSink *sink, int32_t value) {
    if (unlikely(!ensure_size(sink, 4))) return false;
    *((int32_t *) (sink->data + sink->offset)) = value;
    sink->offset += 4;
    return true;
}

bool bit_sink_write_number_int(BitSink *sink, int32_t value) {
    if (unlikely(!ensure_size(sink, 1 + 4))) return false;
    *((int8_t *) (sink->data + sink->offset)) = (int8_t) TYPE_INT;
    sink->offset += 1;
    *((int32_t *) (sink->data + sink->offset)) = value;
    sink->offset += 4;
    return true;
}

bool bit_sink_write_number_double(BitSink *sink, double value) {
    if (unlikely(!ensure_size(sink, 1 + 8))) return false;
    *((int8_t *) (sink->data + sink->offset)) = (int8_t) TYPE_DOUBLE;
    sink->offset += 1;
    *((double *) (sink->data + sink->offset)) = value;
    sink->offset += 8;
    return true;
}

bool bit_sink_write_string_len(BitSink *sink, const char *value, size_t length) {
    if (unlikely(!ensure_size(sink, 4 + length))) return false;
    *((int32_t *) (sink->data + sink->offset)) = (int32_t) length;
    sink->offset += 4;
    // Use strncpy instead of strlcpy, because ending zero isn't welcome here.
    // The length of string is saved.
    strncpy(sink->data + sink->offset, value, length);
    sink->offset += length;
    return true;
}

bool bit_sink_write_ptr(BitSink *sink, void *value) {
    if (unlikely(!ensure_size(sink, 8))) return false;
    *((int64_t *) (sink->data + sink->offset)) = (int64_t) value;
    sink->offset += 8;
    return true;
}
