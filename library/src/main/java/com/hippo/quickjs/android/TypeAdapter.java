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

  public abstract JSValue toJSValue(Depot depot, Context context, @Nullable T value);

  public abstract T fromJSValue(Depot depot, Context context, JSValue value);

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
    public JSValue toJSValue(Depot depot, Context context, T value) {
      if (value == null) return context.createJSNull();
      return delegate.toJSValue(depot, context, value);
    }

    @Override
    public T fromJSValue(Depot depot, Context context, JSValue value) {
      if (value instanceof JSNull || value instanceof JSUndefined) return null;
      return delegate.fromJSValue(depot, context, value);
    }
  }

  public interface Factory {
    @Nullable
    TypeAdapter<?> create(Depot depot, Type type);
  }

  public interface Depot {
    /**
     * Returns a TypeAdapter for the type.
     *
     * @throws IllegalArgumentException if no TypeAdapter matched
     */
    <T> TypeAdapter<T> getAdapter(Type type);
  }

  public interface Context {

    /**
     * Creates a JavaScript undefined.
     */
    JSUndefined createJSUndefined();

    /**
     * Creates a JavaScript null.
     */
    JSNull createJSNull();

    /**
     * Creates a JavaScript boolean.
     */
    JSBoolean createJSBoolean(boolean value);

    /**
     * Creates a JavaScript number.
     */
    JSNumber createJSNumber(int value);

    /**
     * Creates a JavaScript number.
     */
    JSNumber createJSNumber(double value);

    /**
     * Creates a JavaScript string.
     */
    JSString createJSString(String value);

    /**
     * Creates a JavaScript object.
     */
    JSObject createJSObject();

    /**
     * Creates a JavaScript array.
     */
    JSArray createJSArray();

    /**
     * Create a JavaScript function from a java non-static method.
     */
    JSFunction createJSFunction(Object instance, Method method);
  }
}
