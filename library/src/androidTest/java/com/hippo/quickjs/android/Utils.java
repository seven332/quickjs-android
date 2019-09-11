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

import static org.junit.Assert.*;

public class Utils {

  public static <T extends Throwable> void assertException(Class<T> type, String message, Block block) {
    try {
      block.run();
      fail();
    } catch (Throwable e) {
      assertTrue("excepted: " + type.getName() + ", actual: " + e.getClass().getName(), type.isInstance(e));
      assertEquals(message, e.getMessage());
    }
  }

  public interface Block {
    void run();
  }

  public static <T> void assertEquivalent(String script, T except, Class<T> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static <T> void assertEquivalent(String script, T except, GenericType<T> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, boolean[] except, Class<boolean[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, byte[] except, Class<byte[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, char[] except, Class<char[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, short[] except, Class<short[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, int[] except, Class<int[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, long[] except, Class<long[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static void assertEquivalent(String script, float[] except, Class<float[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type), 0.0f);
      }
    }
  }

  public static void assertEquivalent(String script, double[] except, Class<double[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type), 0.0);
      }
    }
  }

  public static <T> void assertEquivalent(String script, T[] except, Class<T[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static <T> void assertEquivalent(String script, T[] except, GenericType<T[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  public static <T> void assertException(String script, Class<T> type, Class<?> exception, String message) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        try {
          context.evaluate(script, "test.js", type);
          fail("Unreached");
        } catch (Throwable e) {
          assertEquals(exception, e.getClass());
          assertEquals(message, e.getMessage());
        }
      }
    }
  }

  public static <T> void assertException(String script, GenericType<T> type, Class<?> exception, String message) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        try {
          context.evaluate(script, "test.js", type);
          fail("Unreached");
        } catch (Throwable e) {
          assertEquals(exception, e.getClass());
          assertEquals(message, e.getMessage());
        }
      }
    }
  }
}
