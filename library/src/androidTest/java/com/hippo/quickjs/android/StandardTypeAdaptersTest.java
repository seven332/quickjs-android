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

import static org.junit.Assert.*;

public class StandardTypeAdaptersTest {

  private <T> void assertEquivalent(String script, T except, Class<T> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertEquals(except, context.evaluate(script, "test.js", StandardTypeAdapters.FACTORY.create(clazz)));
      }
    }
  }

  private <T> void assertException(String script, String message, Class<T> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        try {
          context.evaluate(script, "test.js", StandardTypeAdapters.FACTORY.create(clazz));
          fail();
        } catch (JSDataException e) {
          assertEquals(message, e.getMessage());
        }
      }
    }
  }

  @Test
  public void testBoolean() {
    assertEquivalent("false", false, boolean.class);
    assertEquivalent("true", true, boolean.class);
    assertException("null", "Invalid JSValue tag for boolean: 2", boolean.class);
    assertException("undefined", "Invalid JSValue tag for boolean: 3", boolean.class);
    assertException("1", "Invalid JSValue tag for boolean: 0", boolean.class);

    assertEquivalent("false", false, Boolean.class);
    assertEquivalent("true", true, Boolean.class);
    assertEquivalent("null", null, Boolean.class);
    assertEquivalent("undefined", null, Boolean.class);
    assertException("1", "Invalid JSValue tag for boolean: 0", Boolean.class);
  }

  @Test
  public void testInt() {
    assertEquivalent("0", 0, int.class);
    assertEquivalent("0.0", 0, int.class);
    assertEquivalent("1", 1, int.class);
    assertEquivalent("1.0", 1, int.class);
    assertEquivalent("2147483647", 2147483647, int.class);
    assertEquivalent("-2147483648", -2147483648, int.class);
    assertException("null", "Invalid JSValue tag for int: 2", int.class);
    assertException("undefined", "Invalid JSValue tag for int: 3", int.class);
    assertException("false", "Invalid JSValue tag for int: 1", int.class);

    assertEquivalent("0", 0, Integer.class);
    assertEquivalent("0.0", 0, Integer.class);
    assertEquivalent("1", 1, Integer.class);
    assertEquivalent("1.0", 1, Integer.class);
    assertEquivalent("2147483647", 2147483647, Integer.class);
    assertEquivalent("-2147483648", -2147483648, Integer.class);
    assertEquivalent("null", null, Integer.class);
    assertEquivalent("undefined", null, Integer.class);
    assertException("false", "Invalid JSValue tag for int: 1", Integer.class);
  }

  @Test
  public void testDouble() {
    assertEquivalent("0", 0.0, double.class);
    assertEquivalent("0.0", 0.0, double.class);
    assertEquivalent("1", 1.0, double.class);
    assertEquivalent("1.0", 1.0, double.class);
    assertEquivalent("1.1", 1.1, double.class);
    assertEquivalent("Number.MAX_VALUE", Double.MAX_VALUE, double.class);
    assertEquivalent("Number.MIN_VALUE", Double.MIN_VALUE, double.class);
    assertEquivalent("Number.NaN", Double.NaN, double.class);
    assertException("null", "Invalid JSValue tag for double: 2", double.class);
    assertException("undefined", "Invalid JSValue tag for double: 3", double.class);
    assertException("false", "Invalid JSValue tag for double: 1", double.class);

    assertEquivalent("0", 0.0, Double.class);
    assertEquivalent("0.0", 0.0, Double.class);
    assertEquivalent("1", 1.0, Double.class);
    assertEquivalent("1.0", 1.0, Double.class);
    assertEquivalent("1.1", 1.1, Double.class);
    assertEquivalent("Number.MAX_VALUE", Double.MAX_VALUE, Double.class);
    assertEquivalent("Number.MIN_VALUE", Double.MIN_VALUE, Double.class);
    assertEquivalent("Number.NaN", Double.NaN, Double.class);
    assertEquivalent("null", null, Double.class);
    assertEquivalent("undefined", null, Double.class);
    assertException("false", "Invalid JSValue tag for double: 1", Double.class);
  }

  @Test
  public void testString() {
    assertEquivalent("''", "", String.class);
    assertEquivalent("'str'", "str", String.class);
    assertEquivalent("null", null, String.class);
    assertEquivalent("undefined", null, String.class);
    assertException("false", "Invalid JSValue tag for string: 1", String.class);
  }
}
