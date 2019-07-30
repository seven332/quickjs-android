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

class StandardTypeAdapters {

  static final TypeAdapter.Factory FACTORY = new TypeAdapter.Factory() {

    @Nullable
    @Override
    public TypeAdapter<?> create(Type type) {
      if (type == boolean.class) return BOOLEAN_TYPE_ADAPTER;
      if (type == byte.class) return BYTE_TYPE_ADAPTER;
      if (type == char.class) return CHARACTER_TYPE_ADAPTER;
      if (type == short.class) return SHORT_TYPE_ADAPTER;
      if (type == int.class) return INTEGER_TYPE_ADAPTER;
      if (type == long.class) return LONG_TYPE_ADAPTER;
      if (type == float.class) return FLOAT_TYPE_ADAPTER;
      if (type == double.class) return DOUBLE_TYPE_ADAPTER;
      if (type == Boolean.class) return new NullableTypeAdapter<>(BOOLEAN_TYPE_ADAPTER);
      if (type == Byte.class) return new NullableTypeAdapter<>(BYTE_TYPE_ADAPTER);
      if (type == Character.class) return new NullableTypeAdapter<>(CHARACTER_TYPE_ADAPTER);
      if (type == Short.class) return new NullableTypeAdapter<>(SHORT_TYPE_ADAPTER);
      if (type == Integer.class) return new NullableTypeAdapter<>(INTEGER_TYPE_ADAPTER);
      if (type == Long.class) return new NullableTypeAdapter<>(LONG_TYPE_ADAPTER);
      if (type == Float.class) return new NullableTypeAdapter<>(FLOAT_TYPE_ADAPTER);
      if (type == Double.class) return new NullableTypeAdapter<>(DOUBLE_TYPE_ADAPTER);
      if (type == String.class) return new NullableTypeAdapter<>(STRING_TYPE_ADAPTER);
      return null;
    }
  };

  private static final TypeAdapter<Boolean> BOOLEAN_TYPE_ADAPTER = new TypeAdapter<Boolean>() {
    @Override
    public JSValue toJSValue(Boolean value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Boolean fromJSValue(JSValue value) {
      return value.getBoolean();
    }
  };

  private static final TypeAdapter<Byte> BYTE_TYPE_ADAPTER = new TypeAdapter<Byte>() {
    @Override
    public JSValue toJSValue(Byte value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Byte fromJSValue(JSValue value) {
      return value.getByte();
    }
  };

  private static final TypeAdapter<Character> CHARACTER_TYPE_ADAPTER = new TypeAdapter<Character>() {
    @Override
    public JSValue toJSValue(Character value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Character fromJSValue(JSValue value) {
      return value.getChar();
    }
  };

  private static final TypeAdapter<Short> SHORT_TYPE_ADAPTER = new TypeAdapter<Short>() {
    @Override
    public JSValue toJSValue(Short value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Short fromJSValue(JSValue value) {
      return value.getShort();
    }
  };

  private static final TypeAdapter<Integer> INTEGER_TYPE_ADAPTER = new TypeAdapter<Integer>() {
    @Override
    public JSValue toJSValue(Integer value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Integer fromJSValue(JSValue value) {
      return value.getInt();
    }
  };

  private static final TypeAdapter<Long> LONG_TYPE_ADAPTER = new TypeAdapter<Long>() {
    @Override
    public JSValue toJSValue(Long value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Long fromJSValue(JSValue value) {
      return value.getLong();
    }
  };

  private static final TypeAdapter<Float> FLOAT_TYPE_ADAPTER = new TypeAdapter<Float>() {
    @Override
    public JSValue toJSValue(Float value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Float fromJSValue(JSValue value) {
      return value.getFloat();
    }
  };

  private static final TypeAdapter<Double> DOUBLE_TYPE_ADAPTER = new TypeAdapter<Double>() {
    @Override
    public JSValue toJSValue(Double value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public Double fromJSValue(JSValue value) {
      return value.getDouble();
    }
  };

  private static final TypeAdapter<String> STRING_TYPE_ADAPTER = new TypeAdapter<String>() {
    @Override
    public JSValue toJSValue(String value) {
      return null;
    }

    @Override
    public String fromJSValue(JSValue value) {
      return value.getString();
    }
  };

  private static class NullableTypeAdapter<T> extends TypeAdapter<T> {

    private final TypeAdapter<T> delegate;

    NullableTypeAdapter(TypeAdapter<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public JSValue toJSValue(T value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public T fromJSValue(JSValue value) {
      int tag = value.getType();
      if (tag == JSValue.TYPE_NULL || tag == JSValue.TYPE_UNDEFINED) return null;
      return delegate.fromJSValue(value);
    }
  }
}
