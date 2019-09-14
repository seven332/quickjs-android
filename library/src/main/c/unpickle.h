#ifndef QUICKJS_ANDROID_UNPICKLE_H
#define QUICKJS_ANDROID_UNPICKLE_H

#include <quickjs.h>
#include <stdbool.h>

#include "bit-source.h"
#include "bit-sink.h"

JSValue unpickle(JSContext *ctx, BitSource *command, BitSource *source);

#endif //QUICKJS_ANDROID_UNPICKLE_H
