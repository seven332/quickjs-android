/*
 * Copyright 2019 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.quickjs.android;

import androidx.annotation.Nullable;

import java.lang.reflect.Type;

class StandardTranslators {

  static final Translator.Factory FACTORY = new Translator.Factory() {
    @Nullable
    @Override
    public Translator<?> create(Translator.Depot depot, Type type) {
      if (type == void.class) return VOID_TRANSLATOR;
      if (type == Void.class) return VOID_TRANSLATOR;
      if (type == boolean.class) return BOOLEAN_TRANSLATOR;
      if (type == byte.class) return BYTE_TRANSLATOR;
      if (type == char.class) return CHAR_TRANSLATOR;
      if (type == short.class) return SHORT_TRANSLATOR;
      if (type == int.class) return INT_TRANSLATOR;
      if (type == long.class) return LONG_TRANSLATOR;
      if (type == float.class) return FLOAT_TRANSLATOR;
      if (type == double.class) return DOUBLE_TRANSLATOR;
      if (type instanceof NonNullType) {
        Type nullableType = ((NonNullType) type).getNullableType();
        if (nullableType == String.class) return STRING_TRANSLATOR;
      }
      return null;
    }
  };

  private static final Translator<Void> VOID_TRANSLATOR =
      new Translator<Void>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NULL },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_NULL }
      ) {
        @Override
        protected Void unpickle(BitSource source) {
          source.nextNull();
          return null;
        }

        @Override
        protected void pickle(Void value, BitSink sink) { }
      };

  private static final Translator<Boolean> BOOLEAN_TRANSLATOR =
      new Translator<Boolean>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_BOOLEAN },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_BOOLEAN }
      ) {
        @Override
        protected Boolean unpickle(BitSource source) {
          return source.nextBoolean();
        }

        @Override
        protected void pickle(Boolean value, BitSink sink) {
          sink.writeBoolean(value);
        }
      };

  private static final Translator<Byte> BYTE_TRANSLATOR =
      new Translator<Byte>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NUMBER },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_INT }
      ) {
        @Override
        protected Byte unpickle(BitSource source) {
          return source.nextByte();
        }

        @Override
        protected void pickle(Byte value, BitSink sink) {
          sink.writeInt(value);
        }
      };

  private static final Translator<Character> CHAR_TRANSLATOR =
      new Translator<Character>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_STRING },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_STRING }
      ) {
        @Override
        protected Character unpickle(BitSource source) {
          return source.nextChar();
        }

        @Override
        protected void pickle(Character value, BitSink sink) {
          sink.writeString(value.toString());
        }
      };

  private static final Translator<Short> SHORT_TRANSLATOR =
      new Translator<Short>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NUMBER },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_INT }
      ) {
        @Override
        protected Short unpickle(BitSource source) {
          return source.nextShort();
        }

        @Override
        protected void pickle(Short value, BitSink sink) {
          sink.writeInt(value);
        }
      };

  private static final Translator<Integer> INT_TRANSLATOR =
      new Translator<Integer>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NUMBER },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_INT }
      ) {
        @Override
        protected Integer unpickle(BitSource source) {
          return source.nextInt();
        }

        @Override
        protected void pickle(Integer value, BitSink sink) {
          sink.writeInt(value);
        }
      };

  private static final Translator<Long> LONG_TRANSLATOR =
      new Translator<Long>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NUMBER },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_DOUBLE }
      ) {
        @Override
        protected Long unpickle(BitSource source) {
          return source.nextLong();
        }

        @Override
        protected void pickle(Long value, BitSink sink) {
          // TODO Loss of precision
          sink.writeDouble(value);
        }
      };

  private static final Translator<Float> FLOAT_TRANSLATOR =
      new Translator<Float>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NUMBER },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_DOUBLE }
      ) {
        @Override
        protected Float unpickle(BitSource source) {
          return source.nextFloat();
        }

        @Override
        protected void pickle(Float value, BitSink sink) {
          sink.writeDouble(value);
        }
      };

  private static final Translator<Double> DOUBLE_TRANSLATOR =
      new Translator<Double>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_NUMBER },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_DOUBLE }
      ) {
        @Override
        protected Double unpickle(BitSource source) {
          return source.nextDouble();
        }

        @Override
        protected void pickle(Double value, BitSink sink) {
          sink.writeDouble(value);
        }
      };

  private static final Translator<String> STRING_TRANSLATOR =
      new Translator<String>(
          new byte[] { Translator.PICKLE_FLAG_TYPE_STRING },
          new byte[] { Translator.UNPICKLE_FLAG_TYPE_STRING }
      ) {
        @Override
        protected String unpickle(BitSource source) {
          return source.nextString();
        }

        @Override
        protected void pickle(String value, BitSink sink) {
          sink.writeString(value);
        }
      };
}
