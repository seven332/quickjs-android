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

  private void assertEquivalent(String script, int[] except, Class<int[]> clazz) {
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
    assertEquivalent("[true, false]", new boolean[] { true, false }, boolean[].class);
    assertException("[true, null, false]", "Can't pickle the JSValue", boolean[].class);
    assertException("[true, 2.1, false]", "Can't pickle the JSValue", boolean[].class);
    assertEquivalent("[]", new boolean[] { }, boolean[].class);
    assertEquivalent("null", null, boolean[].class);
    assertEquivalent("undefined", null, boolean[].class);
    assertException("false", "Can't pickle the JSValue", boolean[].class);

    assertEquivalent("[true, false]", new Boolean[] { true, false }, Boolean[].class);
    assertEquivalent("[true, null, false]", new Boolean[] { true, null, false }, Boolean[].class);
    assertException("[true, 2.1, false]", "Can't pickle the JSValue", Boolean[].class);
    assertEquivalent("[]", new Boolean[] { }, Boolean[].class);
    assertEquivalent("null", null, Boolean[].class);
    assertEquivalent("undefined", null, Boolean[].class);
    assertException("false", "Can't pickle the JSValue", Boolean[].class);
  }

  @Test
  public void byteArray() {
    assertEquivalent("[1, 2, 3]", new byte[] { 1, 2, 3 }, byte[].class);
    assertException("[1, null, 3]", "Can't pickle the JSValue", byte[].class);
    assertException("[1, 214321, 3]", "Can't treat 214321 as byte", byte[].class);
    assertEquivalent("[]", new byte[] { }, byte[].class);
    assertEquivalent("null", null, byte[].class);
    assertEquivalent("undefined", null, byte[].class);
    assertException("false", "Can't pickle the JSValue", byte[].class);

    assertEquivalent("[1, 2, 3]", new Byte[] { 1, 2, 3 }, Byte[].class);
    assertEquivalent("[1, null, 3]", new Byte[] { 1, null, 3 }, Byte[].class);
    assertException("[1, 214321, 3]", "Can't treat 214321 as byte", Byte[].class);
    assertEquivalent("[]", new Byte[] { }, Byte[].class);
    assertEquivalent("null", null, Byte[].class);
    assertEquivalent("undefined", null, Byte[].class);
    assertException("false", "Can't pickle the JSValue", Byte[].class);
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

  @Test
  public void floatArray() {
    assertEquivalent("[1.1, 2, 3.5]", new float[] { 1.1f, 2f, 3.5f }, float[].class);
    assertException("[1.1, null, 3.5]", "Can't pickle the JSValue", float[].class);
    assertException("[1.1, false, 3.5]", "Can't pickle the JSValue", float[].class);
    assertEquivalent("[]", new float[] { }, float[].class);
    assertEquivalent("null", null, float[].class);
    assertEquivalent("undefined", null, float[].class);
    assertException("false", "Can't pickle the JSValue", float[].class);

    assertEquivalent("[1.1, 2, 3.5]", new Float[] { 1.1f, 2f, 3.5f }, Float[].class);
    assertEquivalent("[1.1, null, 3.5]", new Float[] { 1.1f, null, 3.5f }, Float[].class);
    assertException("[1.1, false, 3.5]", "Can't pickle the JSValue", Float[].class);
    assertEquivalent("[]", new Float[] { }, Float[].class);
    assertEquivalent("null", null, Float[].class);
    assertEquivalent("undefined", null, Float[].class);
    assertException("false", "Can't pickle the JSValue", Float[].class);
  }

  @Test
  public void stringArray() {
    assertEquivalent("['str', 'ing']", new String[] { "str", "ing" }, String[].class);
    assertEquivalent("['str', null, 'ing']", new String[] { "str", null, "ing" }, String[].class);
    assertException("['str', false, 'ing']", "Can't pickle the JSValue", String[].class);
    assertEquivalent("[]", new String[] { }, String[].class);
    assertEquivalent("null", null, String[].class);
    assertEquivalent("undefined", null, String[].class);
    assertException("false", "Can't pickle the JSValue", String[].class);
  }

  @Test
  public void stringArrayArray() {
    assertEquivalent(
        "[['str', 'ing'], ['st', 'ri', 'ng']]",
        new String[][] { new String[] { "str", "ing" }, new String[] { "st", "ri", "ng" } },
        String[][].class
    );
    assertEquivalent("[]", new String[][] { }, String[][].class);
    assertEquivalent("null", null, String[][].class);
    assertEquivalent("undefined", null, String[][].class);
    assertException("[false]", "Can't pickle the JSValue", String[][].class);
    assertException("[[false]]", "Can't pickle the JSValue", String[][].class);
    assertException("[[[false]]]", "Can't pickle the JSValue", String[][].class);
  }
}
