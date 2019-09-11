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

import static com.hippo.quickjs.android.Utils.assertEquivalent;
import static com.hippo.quickjs.android.Utils.assertException;

public class StandardTranslatorsTest {

  @Test
  public void testBoolean() {
    assertEquivalent("false", false, boolean.class);
    assertEquivalent("true", true, boolean.class);
    assertException("null", boolean.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", boolean.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("1", boolean.class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("false", false, Boolean.class);
    assertEquivalent("true", true, Boolean.class);
    assertEquivalent("null", null, Boolean.class);
    assertEquivalent("undefined", null, Boolean.class);
    assertException("1", Boolean.class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void testByte() {
    assertEquivalent("0", (byte) 0, byte.class);
    assertEquivalent("1", (byte) 1, byte.class);
    assertEquivalent("-1", (byte) -1, byte.class);
    assertEquivalent("1.0", (byte) 1, byte.class);
    assertEquivalent("127", Byte.MAX_VALUE, byte.class);
    assertEquivalent("-128", Byte.MIN_VALUE, byte.class);
    assertException("128", byte.class, JSDataException.class, "Can't treat 128 as byte");
    assertException("-129", byte.class, JSDataException.class, "Can't treat -129 as byte");
    assertException("1.1", byte.class, JSDataException.class, "Can't treat 1.1 as byte");
    assertException("null", byte.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", byte.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", byte.class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("0", (byte) 0, Byte.class);
    assertEquivalent("1", (byte) 1, Byte.class);
    assertEquivalent("-1", (byte) -1, Byte.class);
    assertEquivalent("1.0", (byte) 1, Byte.class);
    assertEquivalent("127", Byte.MAX_VALUE, Byte.class);
    assertEquivalent("-128", Byte.MIN_VALUE, Byte.class);
    assertException("128", Byte.class, JSDataException.class, "Can't treat 128 as byte");
    assertException("-129", Byte.class, JSDataException.class, "Can't treat -129 as byte");
    assertException("1.1", Byte.class, JSDataException.class, "Can't treat 1.1 as byte");
    assertEquivalent("null", null, Byte.class);
    assertEquivalent("undefined", null, Byte.class);
    assertException("false", Byte.class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void testChar() {
    assertEquivalent("'a'", 'a', char.class);
    assertException("'abc'", char.class, JSDataException.class, "Can't treat \"abc\" as char");
    assertException("''", char.class, JSDataException.class, "Can't treat \"\" as char");
    assertException("null", char.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", char.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", char.class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("'a'", 'a', Character.class);
    assertException("'abc'", Character.class, JSDataException.class, "Can't treat \"abc\" as char");
    assertException("''", Character.class, JSDataException.class, "Can't treat \"\" as char");
    assertEquivalent("null", null, Character.class);
    assertEquivalent("undefined", null, Character.class);
    assertException("false", Character.class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void testShort() {
    assertEquivalent("0", (short) 0, short.class);
    assertEquivalent("1", (short) 1, short.class);
    assertEquivalent("-1", (short) -1, short.class);
    assertEquivalent("1.0", (short) 1, short.class);
    assertEquivalent("32767", Short.MAX_VALUE, short.class);
    assertEquivalent("-32768", Short.MIN_VALUE, short.class);
    assertException("32768", short.class, JSDataException.class, "Can't treat 32768 as short");
    assertException("-32769", short.class, JSDataException.class, "Can't treat -32769 as short");
    assertException("1.1", short.class, JSDataException.class, "Can't treat 1.1 as short");
    assertException("null", short.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", short.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", short.class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("0", (short) 0, Short.class);
    assertEquivalent("1", (short) 1, Short.class);
    assertEquivalent("-1", (short) -1, Short.class);
    assertEquivalent("1.0", (short) 1, Short.class);
    assertEquivalent("32767", Short.MAX_VALUE, Short.class);
    assertEquivalent("-32768", Short.MIN_VALUE, Short.class);
    assertException("32768", Short.class, JSDataException.class, "Can't treat 32768 as short");
    assertException("-32769", Short.class, JSDataException.class, "Can't treat -32769 as short");
    assertException("1.1", Short.class, JSDataException.class, "Can't treat 1.1 as short");
    assertEquivalent("null", null, Short.class);
    assertEquivalent("undefined", null, Short.class);
    assertException("false", Short.class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void testInt() {
    assertEquivalent("0", 0, int.class);
    assertEquivalent("0.0", 0, int.class);
    assertEquivalent("1", 1, int.class);
    assertEquivalent("1.0", 1, int.class);
    assertEquivalent("2147483647", Integer.MAX_VALUE, int.class);
    assertEquivalent("-2147483648", Integer.MIN_VALUE, int.class);
    assertException("2147483648", int.class, JSDataException.class, "Can't treat 2.147483648E9 as int");
    assertException("-2147483649", int.class, JSDataException.class, "Can't treat -2.147483649E9 as int");
    assertException("null", int.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", int.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", int.class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("0", 0, Integer.class);
    assertEquivalent("0.0", 0, Integer.class);
    assertEquivalent("1", 1, Integer.class);
    assertEquivalent("1.0", 1, Integer.class);
    assertEquivalent("2147483647", 2147483647, Integer.class);
    assertEquivalent("-2147483648", -2147483648, Integer.class);
    assertException("2147483648", Integer.class, JSDataException.class, "Can't treat 2.147483648E9 as int");
    assertException("-2147483649", Integer.class, JSDataException.class, "Can't treat -2.147483649E9 as int");
    assertEquivalent("null", null, Integer.class);
    assertEquivalent("undefined", null, Integer.class);
    assertException("false", Integer.class, JSDataException.class, "Can't pickle the JSValue");
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
    assertException("0.000001", long.class, JSDataException.class, "Can't treat 1.0E-6 as long");
    assertException("9923372036854775809", long.class, JSDataException.class, "Can't treat 9.923372036854776E18 as long");
    assertException("null", long.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", long.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", long.class, JSDataException.class, "Can't pickle the JSValue");

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
    assertException("0.000001", Long.class, JSDataException.class, "Can't treat 1.0E-6 as long");
    assertException("9923372036854775809", Long.class, JSDataException.class, "Can't treat 9.923372036854776E18 as long");
    assertEquivalent("null", null, Long.class);
    assertEquivalent("undefined", null, Long.class);
    assertException("false", Long.class, JSDataException.class, "Can't pickle the JSValue");
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
    assertException("null", float.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", float.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", float.class, JSDataException.class, "Can't pickle the JSValue");

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
    assertException("false", Float.class, JSDataException.class, "Can't pickle the JSValue");
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
    assertException("null", double.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("undefined", double.class, JSDataException.class, "Can't pickle the JSValue");
    assertException("false", double.class, JSDataException.class, "Can't pickle the JSValue");

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
    assertException("false", Double.class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void testString() {
    assertEquivalent("''", "", String.class);
    assertEquivalent("'str'", "str", String.class);
    assertEquivalent("null", null, String.class);
    assertEquivalent("undefined", null, String.class);
    assertException("false", String.class, JSDataException.class, "Can't pickle the JSValue");
  }
}
