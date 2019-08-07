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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StandardTypeAdaptersTest {

  private <T> void assertEquivalent(String script, T except, Class<T> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertEquals(except, context.evaluate(script, "test.js", StandardTypeAdapters.FACTORY.create(quickJS, clazz)));
      }
    }
  }

  private <T> void assertException(String script, String message, Class<T> clazz) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        try {
          context.evaluate(script, "test.js", StandardTypeAdapters.FACTORY.create(quickJS, clazz));
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
    assertException("null", "expected: JSBoolean, actual: JSNull", boolean.class);
    assertException("undefined", "expected: JSBoolean, actual: JSUndefined", boolean.class);
    assertException("1", "expected: JSBoolean, actual: JSInt", boolean.class);

    assertEquivalent("false", false, Boolean.class);
    assertEquivalent("true", true, Boolean.class);
    assertEquivalent("null", null, Boolean.class);
    assertEquivalent("undefined", null, Boolean.class);
    assertException("1", "expected: JSBoolean, actual: JSInt", Boolean.class);
  }

  @Test
  public void testByte() {
    assertEquivalent("0", (byte) 0, byte.class);
    assertEquivalent("1", (byte) 1, byte.class);
    assertEquivalent("-1", (byte) -1, byte.class);
    assertEquivalent("1.0", (byte) 1, byte.class);
    assertEquivalent("127", Byte.MAX_VALUE, byte.class);
    assertEquivalent("-128", Byte.MIN_VALUE, byte.class);
    assertException("128", "Can't treat 128 as byte", byte.class);
    assertException("-129", "Can't treat -129 as byte", byte.class);
    assertException("1.1", "Can't treat 1.1 as byte", byte.class);
    assertException("null", "expected: JSNumber, actual: JSNull", byte.class);
    assertException("undefined", "expected: JSNumber, actual: JSUndefined", byte.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", byte.class);

    assertEquivalent("0", (byte) 0, Byte.class);
    assertEquivalent("1", (byte) 1, Byte.class);
    assertEquivalent("-1", (byte) -1, Byte.class);
    assertEquivalent("1.0", (byte) 1, Byte.class);
    assertEquivalent("127", Byte.MAX_VALUE, Byte.class);
    assertEquivalent("-128", Byte.MIN_VALUE, Byte.class);
    assertException("128", "Can't treat 128 as byte", Byte.class);
    assertException("-129", "Can't treat -129 as byte", Byte.class);
    assertException("1.1", "Can't treat 1.1 as byte", Byte.class);
    assertEquivalent("null", null, Byte.class);
    assertEquivalent("undefined", null, Byte.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", Byte.class);
  }

  @Test
  public void testChar() {
    assertEquivalent("'a'", 'a', char.class);
    assertException("'abc'", "Can't treat \"abc\" as char", char.class);
    assertException("''", "Can't treat \"\" as char", char.class);
    assertException("null", "expected: JSString, actual: JSNull", char.class);
    assertException("undefined", "expected: JSString, actual: JSUndefined", char.class);
    assertException("false", "expected: JSString, actual: JSBoolean", char.class);

    assertEquivalent("'a'", 'a', Character.class);
    assertException("'abc'", "Can't treat \"abc\" as char", Character.class);
    assertException("''", "Can't treat \"\" as char", Character.class);
    assertEquivalent("null", null, Character.class);
    assertEquivalent("undefined", null, Character.class);
    assertException("false", "expected: JSString, actual: JSBoolean", Character.class);
  }

  @Test
  public void testShort() {
    assertEquivalent("0", (short) 0, short.class);
    assertEquivalent("1", (short) 1, short.class);
    assertEquivalent("-1", (short) -1, short.class);
    assertEquivalent("1.0", (short) 1, short.class);
    assertEquivalent("32767", Short.MAX_VALUE, short.class);
    assertEquivalent("-32768", Short.MIN_VALUE, short.class);
    assertException("32768", "Can't treat 32768 as short", short.class);
    assertException("-32769", "Can't treat -32769 as short", short.class);
    assertException("1.1", "Can't treat 1.1 as short", short.class);
    assertException("null", "expected: JSNumber, actual: JSNull", short.class);
    assertException("undefined", "expected: JSNumber, actual: JSUndefined", short.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", short.class);

    assertEquivalent("0", (short) 0, Short.class);
    assertEquivalent("1", (short) 1, Short.class);
    assertEquivalent("-1", (short) -1, Short.class);
    assertEquivalent("1.0", (short) 1, Short.class);
    assertEquivalent("32767", Short.MAX_VALUE, Short.class);
    assertEquivalent("-32768", Short.MIN_VALUE, Short.class);
    assertException("32768", "Can't treat 32768 as short", Short.class);
    assertException("-32769", "Can't treat -32769 as short", Short.class);
    assertException("1.1", "Can't treat 1.1 as short", Short.class);
    assertEquivalent("null", null, Short.class);
    assertEquivalent("undefined", null, Short.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", Short.class);
  }

  @Test
  public void testInt() {
    assertEquivalent("0", 0, int.class);
    assertEquivalent("0.0", 0, int.class);
    assertEquivalent("1", 1, int.class);
    assertEquivalent("1.0", 1, int.class);
    assertEquivalent("2147483647", Integer.MAX_VALUE, int.class);
    assertEquivalent("-2147483648", Integer.MIN_VALUE, int.class);
    assertException("2147483648", "Can't treat 2.147483648E9 as int", int.class);
    assertException("-2147483649", "Can't treat -2.147483649E9 as int", int.class);
    assertException("null", "expected: JSNumber, actual: JSNull", int.class);
    assertException("undefined", "expected: JSNumber, actual: JSUndefined", int.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", int.class);

    assertEquivalent("0", 0, Integer.class);
    assertEquivalent("0.0", 0, Integer.class);
    assertEquivalent("1", 1, Integer.class);
    assertEquivalent("1.0", 1, Integer.class);
    assertEquivalent("2147483647", 2147483647, Integer.class);
    assertEquivalent("-2147483648", -2147483648, Integer.class);
    assertException("2147483648", "Can't treat 2.147483648E9 as int", Integer.class);
    assertException("-2147483649", "Can't treat -2.147483649E9 as int", Integer.class);
    assertEquivalent("null", null, Integer.class);
    assertEquivalent("undefined", null, Integer.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", Integer.class);
  }

  @Test
  public void testLong() {
    assertEquivalent("0", 0L, long.class);
    assertEquivalent("0.0", 0L, long.class);
    assertEquivalent("1", 1L, long.class);
    assertEquivalent("1.0", 1L, long.class);
    assertEquivalent("9007199254740991", 9007199254740991L, long.class);
    assertEquivalent("9007199254740992", 9007199254740992L, long.class);
    assertEquivalent("9007199254740993", 9007199254740992L, long.class);
    assertEquivalent("-9007199254740991", -9007199254740991L, long.class);
    assertEquivalent("-9007199254740992", -9007199254740992L, long.class);
    assertEquivalent("-9007199254740993", -9007199254740992L, long.class);
    assertEquivalent("9223372036854775808", 9223372036854775807L, long.class);
    assertEquivalent("9223372036854775807", 9223372036854775807L, long.class);
    assertEquivalent("9223372036854775806", 9223372036854775807L, long.class);
    assertEquivalent("-9223372036854775807", -9223372036854775808L, long.class);
    assertEquivalent("-9223372036854775808", -9223372036854775808L, long.class);
    assertEquivalent("-9223372036854775809", -9223372036854775808L, long.class);
    assertException("0.000001", "Can't treat 1.0E-6 as long", long.class);
    assertException("9923372036854775809", "Can't treat 9.923372036854776E18 as long", long.class);
    assertException("null", "expected: JSNumber, actual: JSNull", long.class);
    assertException("undefined", "expected: JSNumber, actual: JSUndefined", long.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", long.class);

    assertEquivalent("0", 0L, Long.class);
    assertEquivalent("0.0", 0L, Long.class);
    assertEquivalent("1", 1L, Long.class);
    assertEquivalent("1.0", 1L, Long.class);
    assertEquivalent("9007199254740991", 9007199254740991L, Long.class);
    assertEquivalent("9007199254740992", 9007199254740992L, Long.class);
    assertEquivalent("9007199254740993", 9007199254740992L, Long.class);
    assertEquivalent("-9007199254740991", -9007199254740991L, Long.class);
    assertEquivalent("-9007199254740992", -9007199254740992L, Long.class);
    assertEquivalent("-9007199254740993", -9007199254740992L, Long.class);
    assertEquivalent("9223372036854775808", 9223372036854775807L, Long.class);
    assertEquivalent("9223372036854775807", 9223372036854775807L, Long.class);
    assertEquivalent("9223372036854775806", 9223372036854775807L, Long.class);
    assertEquivalent("-9223372036854775807", -9223372036854775808L, Long.class);
    assertEquivalent("-9223372036854775808", -9223372036854775808L, Long.class);
    assertEquivalent("-9223372036854775809", -9223372036854775808L, Long.class);
    assertException("0.000001", "Can't treat 1.0E-6 as long", Long.class);
    assertException("9923372036854775809", "Can't treat 9.923372036854776E18 as long", Long.class);
    assertEquivalent("null", null, Long.class);
    assertEquivalent("undefined", null, Long.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", Long.class);
  }

  @Test
  public void testFloat() {
    assertEquivalent("0", 0.0f, float.class);
    assertEquivalent("0.0", 0.0f, float.class);
    assertEquivalent("1", 1.0f, float.class);
    assertEquivalent("1.0", 1.0f, float.class);
    assertEquivalent("1.1", 1.1f, float.class);
    assertEquivalent("Number.MAX_VALUE", Float.POSITIVE_INFINITY, float.class);
    assertEquivalent("Number.MIN_VALUE", 0.0f, float.class);
    assertEquivalent("Number.NaN", Float.NaN, float.class);
    assertException("null", "expected: JSNumber, actual: JSNull", float.class);
    assertException("undefined", "expected: JSNumber, actual: JSUndefined", float.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", float.class);

    assertEquivalent("0", 0.0f, Float.class);
    assertEquivalent("0.0", 0.0f, Float.class);
    assertEquivalent("1", 1.0f, Float.class);
    assertEquivalent("1.0", 1.0f, Float.class);
    assertEquivalent("1.1", 1.1f, Float.class);
    assertEquivalent("Number.MAX_VALUE", Float.POSITIVE_INFINITY, Float.class);
    assertEquivalent("Number.MIN_VALUE", 0.0f, Float.class);
    assertEquivalent("Number.NaN", Float.NaN, Float.class);
    assertEquivalent("null", null, Float.class);
    assertEquivalent("undefined", null, Float.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", Float.class);
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
    assertEquivalent("Number.POSITIVE_INFINITY", Double.POSITIVE_INFINITY, double.class);
    assertEquivalent("Number.NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, double.class);
    assertEquivalent("Number.NaN", Double.NaN, double.class);
    assertException("null", "expected: JSNumber, actual: JSNull", double.class);
    assertException("undefined", "expected: JSNumber, actual: JSUndefined", double.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", double.class);

    assertEquivalent("0", 0.0, Double.class);
    assertEquivalent("0.0", 0.0, Double.class);
    assertEquivalent("1", 1.0, Double.class);
    assertEquivalent("1.0", 1.0, Double.class);
    assertEquivalent("1.1", 1.1, Double.class);
    assertEquivalent("Number.MAX_VALUE", Double.MAX_VALUE, Double.class);
    assertEquivalent("Number.MIN_VALUE", Double.MIN_VALUE, Double.class);
    assertEquivalent("Number.POSITIVE_INFINITY", Double.POSITIVE_INFINITY, Double.class);
    assertEquivalent("Number.NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, Double.class);
    assertEquivalent("Number.NaN", Double.NaN, Double.class);
    assertEquivalent("null", null, Double.class);
    assertEquivalent("undefined", null, Double.class);
    assertException("false", "expected: JSNumber, actual: JSBoolean", Double.class);
  }

  @Test
  public void testString() {
    assertEquivalent("''", "", String.class);
    assertEquivalent("'str'", "str", String.class);
    assertEquivalent("null", null, String.class);
    assertEquivalent("undefined", null, String.class);
    assertException("false", "expected: JSString, actual: JSBoolean", String.class);
  }
}
