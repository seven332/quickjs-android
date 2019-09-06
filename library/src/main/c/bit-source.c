#include <assert.h>
#include <malloc.h>
#include <uchar.h>

#include "bit-source.h"

struct BitSource {
    void *data;
    size_t offset;
    size_t size;
};

BitSource *create_source_bit(void *data, size_t size) {
    BitSource *source = malloc(sizeof(BitSource));
    if (source == NULL) {
        return NULL;
    }

    source->data = data;
    source->offset = 0;
    source->size = size;

    return source;
}

#define FUNCTION_NEXT_DATA(NAME, TYPE, SIZE)                 \
TYPE NAME(BitSource *source) {                                  \
    assert(source->offset + (SIZE) <= source->size);         \
    TYPE result = *(TYPE *) (source->data + source->offset); \
    source->offset += (SIZE);                                \
    return result;                                           \
}

FUNCTION_NEXT_DATA(bit_source_next_int8, int8_t, 1);

FUNCTION_NEXT_DATA(bit_source_next_int32, int32_t, 4);

FUNCTION_NEXT_DATA(bit_source_next_int64, int64_t, 8);

FUNCTION_NEXT_DATA(bit_source_next_double, double, 4);

const char *bit_source_next_string(BitSource *source) {
    int32_t len = bit_source_next_int32(source);
    assert(source->offset + len <= source->size);
    const char *str = (const char *) (source->data + source->offset);
    source->offset += len;
    return str;
}

bool bit_source_has_next(BitSource *source) {
    return source->offset < source->size;
}

void bit_source_skip(BitSource *source, size_t step) {
    assert(source->offset + step <= source->size);
    source->offset += step;
}

size_t bit_source_get_offset(BitSource *source) {
    return source->offset;
}

size_t bit_source_get_size(BitSource *source) {
    return source->size;
}

void bit_source_reconfig(BitSource *source, size_t offset, size_t size) {
    source->offset = offset;
    source->size = size;
}

void destroy_source(BitSource *source) {
    free(source);
}
