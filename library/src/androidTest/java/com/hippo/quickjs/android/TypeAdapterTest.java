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
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TypeAdapterTest {

  @Test
  public void testRegisterTypeAdapter() {
    QuickJS quickJS = new QuickJS.Builder().registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter()).build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        AtomicInteger atomicInteger = context.evaluate("1", "test.js", AtomicInteger.class);
        assertEquals(1, atomicInteger.get());
      }
    }
  }

  @Test
  public void testRegisterTypeAdapterFactory() {
    QuickJS quickJS = new QuickJS.Builder().registerTypeAdapterFactory(new AtomicIntegerTypeAdapterFactory()).build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        AtomicInteger atomicInteger = context.evaluate("1", "test.js", AtomicInteger.class);
        assertEquals(1, atomicInteger.get());
      }
    }
  }

  private static class AtomicIntegerTypeAdapter extends TypeAdapter<AtomicInteger> {
    @Override
    public JSValue toJSValue(AtomicInteger value) {
      throw new IllegalStateException("TODO");
    }

    @Override
    public AtomicInteger fromJSValue(JSValue value) {
      return new AtomicInteger(value.cast(JSNumber.class).getInt());
    }
  }

  private static class AtomicIntegerTypeAdapterFactory implements TypeAdapter.Factory {
    @Nullable
    @Override
    public TypeAdapter<?> create(Type type) {
      if (type == AtomicInteger.class) return new AtomicIntegerTypeAdapter();
      return null;
    }
  }
}
