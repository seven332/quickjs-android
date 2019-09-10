#include <malloc.h>
#include <uchar.h>

#include "bit-source.h"

#define FUNCTION_NEXT_DATA(NAME, TYPE, SIZE)                 \
TYPE NAME(BitSource *source) {                               \
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
