#ifndef QUICKJS_ANDROID_BIT_SOURCE_H
#define QUICKJS_ANDROID_BIT_SOURCE_H

#include <stdbool.h>
#include <stdint.h>
#include <assert.h>

#include "common.h"

#define CREATE_BIT_SOURCE(DATA, SIZE) \
    (BitSource) {                     \
        .data = (void *) (DATA),      \
        .offset = 0,                  \
        .size = (SIZE)                \
    }

#define CREATE_COMMAND_BIT_SOURCE(COMMAND)          \
    (BitSource) {                                   \
        .data = (void *) (COMMAND) + sizeof(jsize), \
        .offset = 0,                                \
        .size = (size_t) *(jsize *) (COMMAND)       \
    }

typedef struct BitSource {
    void *data;
    size_t offset;
    size_t size;
} BitSource;

int8_t bit_source_next_int8(BitSource *source);

int16_t bit_source_next_int16(BitSource *source);

int32_t bit_source_next_int32(BitSource *source);

int64_t bit_source_next_int64(BitSource *source);

float bit_source_next_float(BitSource *source);

double bit_source_next_double(BitSource *source);

const char *bit_source_next_string(BitSource *source);

static force_inline bool bit_source_has_next(BitSource *source) {
    return source->offset < source->size;
}

static force_inline void bit_source_skip(BitSource *source, size_t step) {
    assert(source->offset + step <= source->size);
    source->offset += step;
}

static force_inline size_t bit_source_get_offset(BitSource *source) {
    return source->offset;
}

static force_inline size_t bit_source_get_size(BitSource *source) {
    return source->size;
}

static force_inline void bit_source_reconfig(BitSource *source, size_t offset, size_t size) {
    source->offset = offset;
    source->size = size;
}

#endif //QUICKJS_ANDROID_BIT_SOURCE_H
