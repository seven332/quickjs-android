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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ArrayTypeAdapterTest {

  private void assertEquivalent(String script, boolean[] except, Class<boolean[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertEquivalent(String script, byte[] except, Class<byte[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertEquivalent(String script, char[] except, Class<char[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertEquivalent(String script, short[] except, Class<short[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertEquivalent(String script, int[] except, Class<int[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertEquivalent(String script, long[] except, Class<long[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertEquivalent(String script, float[] except, Class<float[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz), 0.0f);
      }
    }
  }

  private void assertEquivalent(String script, double[] except, Class<double[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz), 0.0);
      }
    }
  }

  private <T> void assertEquivalent(String script, T[] except, Class<T[]> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", clazz));
      }
    }
  }

  private void assertException(String script, String message, Class<?> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        try {
          context.evaluate(script, "test.js", clazz);
          fail();
        } catch (JSDataException e) {
          assertEquals(message, e.getMessage());
        }
      }
    }
  }

  @Test
  public void booleanArray() {

  }

  @Test
  public void intArray() {
    assertEquivalent("[1, 2, 3]", new int[] { 1, 2, 3 }, int[].class);
    assertException("[1, null, 3]", "Can't pickle the JSValue", int[].class);
    assertException("[1, 2.1, 3]", "Can't treat 2.1 as int", int[].class);
    assertEquivalent("[]", new int[] { }, int[].class);
    assertEquivalent("null", null, int[].class);
    assertEquivalent("undefined", null, int[].class);
    assertException("false", "Can't pickle the JSValue", int[].class);

    assertEquivalent("[1, 2, 3]", new Integer[] { 1, 2, 3 }, Integer[].class);
    assertEquivalent("[1, null, 3]", new Integer[] { 1, null, 3 }, Integer[].class);
    assertException("[1, 2.1, 3]", "Can't treat 2.1 as int", Integer[].class);
    assertEquivalent("[]", new Integer[] { }, Integer[].class);
    assertEquivalent("null", null, Integer[].class);
    assertEquivalent("undefined", null, Integer[].class);
    assertException("false", "Can't pickle the JSValue", Integer[].class);
  }
}
