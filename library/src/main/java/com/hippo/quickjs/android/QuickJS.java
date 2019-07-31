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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QuickJS is a resources container to create {@link JSRuntime}s.
 */
public class QuickJS implements TypeAdapter.Depot {

  private static final List<TypeAdapter.Factory> BUILT_IN_FACTORIES = new ArrayList<>(1);

  static {
    BUILT_IN_FACTORIES.add(StandardTypeAdapters.FACTORY);
  }

  private final List<TypeAdapter.Factory> factories;
  private final Map<Type, TypeAdapter<?>> adapterCache;

  private QuickJS(QuickJS.Builder builder) {
    List<TypeAdapter.Factory> factories = new ArrayList<>(builder.factories.size() + BUILT_IN_FACTORIES.size());
    factories.addAll(builder.factories);
    factories.addAll(BUILT_IN_FACTORIES);
    this.factories = Collections.unmodifiableList(factories);
    this.adapterCache = new ConcurrentHashMap<>();
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public <T> TypeAdapter<T> getAdapter(Type type) {
    // TODO Create key from type to ensure the same type creates the same key
    TypeAdapter<?> adapter = adapterCache.get(type);
    if (adapter != null) {
      return (TypeAdapter<T>) adapter;
    }

    for (int i = 0, size = factories.size(); i < size; i++) {
      adapter = factories.get(i).create(type);
      if (adapter != null) {
        adapterCache.put(type, adapter);
        return (TypeAdapter<T>) adapter;
      }
    }

    return null;
  }

  /**
   * Creates a JSRuntime with resources in this QuickJS.
   */
  public JSRuntime createJSRuntime() {
    long runtime = QuickJS.createRuntime();
    if (runtime == 0) {
      throw new IllegalStateException("Cannot create JSRuntime instance");
    }
    return new JSRuntime(runtime, this);
  }

  public static class Builder {

    private List<TypeAdapter.Factory> factories = new ArrayList<>();

    public <T> Builder registerTypeAdapter(final Type type, final TypeAdapter<T> adapter) {
      return registerTypeAdapterFactory(new TypeAdapter.Factory() {
        @Nullable
        @Override
        public TypeAdapter<?> create(Type targetType) {
          // TODO Use a custom function to compare type
          if (type == targetType) {
            return adapter;
          }
          return null;
        }
      });
    }

    public Builder registerTypeAdapterFactory(TypeAdapter.Factory factory) {
      factories.add(factory);
      return this;
    }

    public QuickJS build() {
      return new QuickJS(this);
    }
  }

  static {
    System.loadLibrary("quickjs-android");
  }

  static native long createRuntime();
  static native void destroyRuntime(long runtime);

  static native long createContext(long runtime);
  static native void destroyContext(long context);

  static native int getValueTag(long value);
  static native boolean isValueArray(long context, long value);
  static native long getValueProperty(long context, long value, int index);
  static native long getValueProperty(long context, long value, String name);
  static native boolean getValueBoolean(long value);
  static native int getValueInt(long value);
  static native double getValueDouble(long value);
  static native String getValueString(long context, long value);
  static native void destroyValue(long context, long value);

  static native JSException getException(long context);

  static native long evaluate(long context, String sourceCode, String fileName, int flags);
}
