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

public abstract class TypeAdapter<T> {
  /**
   * Converts the java value to {@code JSValue}.
   * Throws {@link JSDataException} if the value can't be handled.
   */
  public abstract JSValue toJSValue(JSContext context, T value);

  /**
   * Converts the {@code JSValue} to java value.
   */
  public abstract T fromJSValue(JSContext context, JSValue value);

  /**
   * Returns a TypeAdapter equal to this TypeAdapter,
   * but with support for null java object and null/undefined javascript value.
   */
  public final TypeAdapter<T> nullable() {
    return new NullableTypeAdapter<>(this);
  }

  private static class NullableTypeAdapter<T> extends TypeAdapter<T> {

    private final TypeAdapter<T> delegate;

    NullableTypeAdapter(TypeAdapter<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public JSValue toJSValue(JSContext context, T value) {
      if (value == null) return context.createJSNull();
      return delegate.toJSValue(context, value);
    }

    @Override
    public T fromJSValue(JSContext context, JSValue value) {
      if (value instanceof JSNull || value instanceof JSUndefined) return null;
      return delegate.fromJSValue(context, value);
    }
  }

  public interface Factory {
    @Nullable
    TypeAdapter<?> create(QuickJS quickJS, Type type);
  }
}
