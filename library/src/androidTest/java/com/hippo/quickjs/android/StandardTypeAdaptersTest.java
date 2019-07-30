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
    assertException("null", "Can't treat the JSValue as boolean, it's type is 2", boolean.class);
    assertException("undefined", "Can't treat the JSValue as boolean, it's type is 3", boolean.class);
    assertException("1", "Can't treat the JSValue as boolean, it's type is 0", boolean.class);

    assertEquivalent("false", false, Boolean.class);
    assertEquivalent("true", true, Boolean.class);
    assertEquivalent("null", null, Boolean.class);
    assertEquivalent("undefined", null, Boolean.class);
    assertException("1", "Can't treat the JSValue as boolean, it's type is 0", Boolean.class);
  }

  @Test
  public void testByte() {
    assertEquivalent("0", (byte) 0, byte.class);
    assertEquivalent("1", (byte) 1, byte.class);
    assertEquivalent("-1", (byte) -1, byte.class);
    assertEquivalent("1.0", (byte) 1, byte.class);
    assertException("9999", "Can't treat the number as byte: 9999", byte.class);
    assertException("1.1", "Can't treat the number as byte: 1.1", byte.class);
    assertException("null", "Can't treat the JSValue as byte, it's type is 2", byte.class);
    assertException("undefined", "Can't treat the JSValue as byte, it's type is 3", byte.class);
    assertException("false", "Can't treat the JSValue as byte, it's type is 1", byte.class);

    assertEquivalent("0", (byte) 0, Byte.class);
    assertEquivalent("1", (byte) 1, Byte.class);
    assertEquivalent("-1", (byte) -1, Byte.class);
    assertEquivalent("1.0", (byte) 1, Byte.class);
    assertException("9999", "Can't treat the number as byte: 9999", Byte.class);
    assertException("1.1", "Can't treat the number as byte: 1.1", Byte.class);
    assertEquivalent("null", null, Byte.class);
    assertEquivalent("undefined", null, Byte.class);
    assertException("false", "Can't treat the JSValue as byte, it's type is 1", Byte.class);
  }

  @Test
  public void testChar() {
    assertEquivalent("'a'", 'a', char.class);
    assertException("'abc'", "Can't treat the string as a char: \"abc\"", char.class);
    assertException("''", "Can't treat the string as a char: \"\"", char.class);
    assertException("null", "Can't treat the JSValue as char, it's type is 2", char.class);
    assertException("undefined", "Can't treat the JSValue as char, it's type is 3", char.class);
    assertException("false", "Can't treat the JSValue as char, it's type is 1", char.class);

    assertEquivalent("'a'", 'a', Character.class);
    assertException("'abc'", "Can't treat the string as a char: \"abc\"", Character.class);
    assertException("''", "Can't treat the string as a char: \"\"", Character.class);
    assertEquivalent("null", null, Character.class);
    assertEquivalent("undefined", null, Character.class);
    assertException("false", "Can't treat the JSValue as char, it's type is 1", Character.class);
  }

  @Test
  public void testShort() {
    assertEquivalent("0", (short) 0, short.class);
    assertEquivalent("1", (short) 1, short.class);
    assertEquivalent("-1", (short) -1, short.class);
    assertEquivalent("1.0", (short) 1, short.class);
    assertException("9999999", "Can't treat the number as short: 9999999", short.class);
    assertException("1.1", "Can't treat the number as short: 1.1", short.class);
    assertException("null", "Can't treat the JSValue as short, it's type is 2", short.class);
    assertException("undefined", "Can't treat the JSValue as short, it's type is 3", short.class);
    assertException("false", "Can't treat the JSValue as short, it's type is 1", short.class);

    assertEquivalent("0", (short) 0, Short.class);
    assertEquivalent("1", (short) 1, Short.class);
    assertEquivalent("-1", (short) -1, Short.class);
    assertEquivalent("1.0", (short) 1, Short.class);
    assertException("9999999", "Can't treat the number as short: 9999999", Short.class);
    assertException("1.1", "Can't treat the number as short: 1.1", Short.class);
    assertEquivalent("null", null, Byte.class);
    assertEquivalent("undefined", null, Byte.class);
    assertException("false", "Can't treat the JSValue as short, it's type is 1", Short.class);
  }

  @Test
  public void testInt() {
    assertEquivalent("0", 0, int.class);
    assertEquivalent("0.0", 0, int.class);
    assertEquivalent("1", 1, int.class);
    assertEquivalent("1.0", 1, int.class);
    assertEquivalent("2147483647", 2147483647, int.class);
    assertEquivalent("-2147483648", -2147483648, int.class);
    assertException("2147483648", "Can't treat the number as int: 2.147483648E9", int.class);
    assertException("-2147483649", "Can't treat the number as int: -2.147483649E9", int.class);
    assertException("null", "Can't treat the JSValue as int, it's type is 2", int.class);
    assertException("undefined", "Can't treat the JSValue as int, it's type is 3", int.class);
    assertException("false", "Can't treat the JSValue as int, it's type is 1", int.class);

    assertEquivalent("0", 0, Integer.class);
    assertEquivalent("0.0", 0, Integer.class);
    assertEquivalent("1", 1, Integer.class);
    assertEquivalent("1.0", 1, Integer.class);
    assertEquivalent("2147483647", 2147483647, Integer.class);
    assertEquivalent("-2147483648", -2147483648, Integer.class);
    assertException("2147483648", "Can't treat the number as int: 2.147483648E9", Integer.class);
    assertException("-2147483649", "Can't treat the number as int: -2.147483649E9", Integer.class);
    assertEquivalent("null", null, Integer.class);
    assertEquivalent("undefined", null, Integer.class);
    assertException("false", "Can't treat the JSValue as int, it's type is 1", Integer.class);
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
    assertException("0.000001", "Can't treat the number as long: 1.0E-6", long.class);
    assertException("9923372036854775809", "Can't treat the number as long: 9.923372036854776E18", long.class);
    assertException("null", "Can't treat the JSValue as long, it's type is 2", long.class);
    assertException("undefined", "Can't treat the JSValue as long, it's type is 3", long.class);
    assertException("false", "Can't treat the JSValue as long, it's type is 1", long.class);

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
    assertException("0.000001", "Can't treat the number as long: 1.0E-6", Long.class);
    assertException("9923372036854775809", "Can't treat the number as long: 9.923372036854776E18", Long.class);
    assertEquivalent("null", null, Long.class);
    assertEquivalent("undefined", null, Long.class);
    assertException("false", "Can't treat the JSValue as long, it's type is 1", Long.class);
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
    assertException("null", "Can't treat the JSValue as float, it's type is 2", float.class);
    assertException("undefined", "Can't treat the JSValue as float, it's type is 3", float.class);
    assertException("false", "Can't treat the JSValue as float, it's type is 1", float.class);

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
    assertException("false", "Can't treat the JSValue as float, it's type is 1", Float.class);
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
    assertException("null", "Can't treat the JSValue as double, it's type is 2", double.class);
    assertException("undefined", "Can't treat the JSValue as double, it's type is 3", double.class);
    assertException("false", "Can't treat the JSValue as double, it's type is 1", double.class);

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
    assertException("false", "Can't treat the JSValue as double, it's type is 1", Double.class);
  }

  @Test
  public void testString() {
    assertEquivalent("''", "", String.class);
    assertEquivalent("'str'", "str", String.class);
    assertEquivalent("null", null, String.class);
    assertEquivalent("undefined", null, String.class);
    assertException("false", "Can't treat the JSValue as string, it's type is 1", String.class);
  }
}
