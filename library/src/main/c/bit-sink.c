#include <malloc.h>
#include <string.h>

#include "bit-sink.h"

#define TYPE_NULL    0
#define TYPE_BOOLEAN 1
#define TYPE_INT     2
#define TYPE_DOUBLE  3
#define TYPE_STRING  4

struct BitSink {
    void *data;
    size_t offset;
    size_t size;
};

BitSink *create_bit_sink(size_t size) {
    BitSink *sink = malloc(sizeof(BitSink));
    if (sink == NULL) {
        return NULL;
    }

    sink->data = malloc(size);
    if (sink->data == NULL) {
        free(sink);
        return NULL;
    }

    sink->offset = 0;
    sink->size = size;

    return sink;
}

static bool ensure_size(BitSink *sink, size_t size) {
    if (sink->offset + size <= sink->size) {
        return true;
    }

    // TODO overflow
    size_t new_size = (sink->offset + size) << 1U;

    void *new_data = realloc(sink->data, new_size);
    if (new_data == NULL) {
        return false;
    }

    sink->data = new_data;
    sink->size = new_size;

    return true;
}

bool bit_sink_write_null(BitSink *sink) {
    if (!ensure_size(sink, 1)) {
        return false;
    }
    // Type
    *((int8_t *) (sink->data + sink->offset)) = (int8_t) TYPE_NULL;
    sink->offset += 1;
    return true;
}

#define FUNCTION_WRITE_DATA(NAME, TYPE, TYPE_I, SIZE)              \
bool NAME(BitSink *sink, TYPE value) {                             \
    if (!ensure_size(sink, 1 + (SIZE))) {                          \
        return false;                                              \
    }                                                              \
    *((int8_t *) (sink->data + sink->offset)) = (int8_t) (TYPE_I); \
    sink->offset += 1;                                             \
    *((TYPE *) (sink->data + sink->offset)) = value;               \
    sink->offset += (SIZE);                                        \
    return true;                                                   \
}

FUNCTION_WRITE_DATA(bit_sink_write_boolean, int8_t, TYPE_BOOLEAN, 1);

FUNCTION_WRITE_DATA(bit_sink_write_int, int32_t, TYPE_INT, 4);

FUNCTION_WRITE_DATA(bit_sink_write_double, double, TYPE_DOUBLE, 8);

bool bit_sink_write_string_len(BitSink *sink, const char *value, size_t length) {
    if (!ensure_size(sink, 1 + 4 + length)) {
        return false;
    }
    // Type
    *((int8_t *) (sink->data + sink->offset)) = (int8_t) TYPE_STRING;
    sink->offset += 1;
    // Length
    *((int32_t *) (sink->data + sink->offset)) = (int32_t) length;
    sink->offset += 4;
    // String
    // Use strncpy instead of strlcpy, because ending zero isn't welcome here.
    // The length of string is saved.
    strncpy(sink->data + sink->offset, value, length);
    sink->offset += length;
    return true;
}

size_t bit_sink_get_length(BitSink *sink) {
    return sink->offset;
}

void *bit_sink_get_data(BitSink *sink) {
    return sink->data;
}

void destroy_bit_sink(BitSink *sink) {
    free(sink->data);
    free(sink);
}
