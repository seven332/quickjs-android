#ifndef QUICKJS_ANDROID_BIT_SOURCE_H
#define QUICKJS_ANDROID_BIT_SOURCE_H

#include <stdbool.h>

typedef struct BitSource BitSource;

BitSource *create_source_bit(void *data, size_t size);

int8_t bit_source_next_int8(BitSource *source);

int32_t bit_source_next_int32(BitSource *source);

int64_t bit_source_next_int64(BitSource *source);

double bit_source_next_double(BitSource *source);

const char *bit_source_next_string(BitSource *source);

bool bit_source_has_next(BitSource *source);

void bit_source_skip(BitSource *source, size_t step);

size_t bit_source_get_offset(BitSource *source);

size_t bit_source_get_size(BitSource *source);

void bit_source_reconfig(BitSource *source, size_t offset, size_t size);

void destroy_source(BitSource *source);

#endif //QUICKJS_ANDROID_BIT_SOURCE_H
