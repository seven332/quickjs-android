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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

import android.util.ArrayMap;

public class TypeAdapterTest {

  @Test
  public void registerTypeAdapter() {
    QuickJS quickJS = new QuickJS.Builder().registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter()).build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        AtomicInteger atomicInteger = context.evaluate("1", "test.js", AtomicInteger.class);
        assertEquals(1, atomicInteger.get());
      }
    }
  }

  @Test
  public void registerTypeAdapterFactory() {
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
    public JSValue toJSValue(JSContext context, AtomicInteger value) {
      return context.createJSNumber(value.get());
    }

    @Override
    public AtomicInteger fromJSValue(JSContext context, JSValue value) {
      return new AtomicInteger(value.cast(JSNumber.class).getInt());
    }
  }

  private static class AtomicIntegerTypeAdapterFactory implements TypeAdapter.Factory {
    @Nullable
    @Override
    public TypeAdapter<?> create(QuickJS quickJS, Type type) {
      if (type == AtomicInteger.class) return new AtomicIntegerTypeAdapter();
      return null;
    }
  }

  @Test
  public void registerMapAdapter() {
    Type mapType = new JavaType<Map<String, String>>() {}.type;

    QuickJS quickJS = new QuickJS.Builder()
        .registerTypeAdapter(mapType, new MapTypeAdapter().nullable())
        .build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        Map<String, String> actual = context.evaluate(
            "a = { key1: \"value1\", key2: \"value2\" }", "test.js", mapType);
        Map<String, String> expected = new ArrayMap<>();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        assertEquals(expected, actual);
      }
    }
  }

  private static class MapTypeAdapter extends TypeAdapter<Map<String, String>> {
    @Override
    public JSValue toJSValue(JSContext context, Map<String, String> value) {
      JSObject jo = context.createJSObject();
      value.forEach((k, v) -> {
        if (k == null) return;
        jo.setProperty(k, context.createJSString(v));
      });
      return jo;
    }

    @Override
    public Map<String, String> fromJSValue(JSContext context, JSValue value) {
      JSObject jo = value.cast(JSObject.class);
      JSFunction keysFunction = context.getGlobalObject()
          .getProperty("Object").cast(JSObject.class)
          .getProperty("keys").cast(JSFunction.class);

      TypeAdapter<String[]> adapter = context.quickJS.getAdapter(String[].class);
      JSValue keysResult = keysFunction.invoke(null, new JSValue[] { jo });
      String[] keys = adapter.fromJSValue(context, keysResult);

      Map<String, String> map = new ArrayMap<>(keys.length);
      for (String key: keys) {
        String val = jo.getProperty(key).cast(JSString.class).getString();
        map.put(key, val);
      }

      return map;
    }
  }
}
