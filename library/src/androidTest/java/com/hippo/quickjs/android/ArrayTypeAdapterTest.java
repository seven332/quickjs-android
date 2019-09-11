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

public class ArrayTypeAdapterTest {

  @Test
  public void booleanArray() {
    assertEquivalent("[true, false]", new boolean[] { true, false }, boolean[].class);
    assertException("[true, null, false]", boolean[].class, JSDataException.class, "Can't pickle the JSValue");
    assertException("[true, 2.1, false]", boolean[].class, JSDataException.class, "Can't pickle the JSValue");
    assertEquivalent("[]", new boolean[] { }, boolean[].class);
    assertEquivalent("null", null, boolean[].class);
    assertEquivalent("undefined", null, boolean[].class);
    assertException("false", boolean[].class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("[true, false]", new Boolean[] { true, false }, Boolean[].class);
    assertEquivalent("[true, null, false]", new Boolean[] { true, null, false }, Boolean[].class);
    assertException("[true, 2.1, false]", Boolean[].class, JSDataException.class, "Can't pickle the JSValue");
    assertEquivalent("[]", new Boolean[] { }, Boolean[].class);
    assertEquivalent("null", null, Boolean[].class);
    assertEquivalent("undefined", null, Boolean[].class);
    assertException("false", Boolean[].class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void byteArray() {
    assertEquivalent("[1, 2, 3]", new byte[] { 1, 2, 3 }, byte[].class);
    assertException("[1, null, 3]", byte[].class, JSDataException.class, "Can't pickle the JSValue");
    assertException("[1, 214321, 3]", byte[].class, JSDataException.class, "Can't treat 214321 as byte");
    assertEquivalent("[]", new byte[] { }, byte[].class);
    assertEquivalent("null", null, byte[].class);
    assertEquivalent("undefined", null, byte[].class);
    assertException("false", byte[].class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("[1, 2, 3]", new Byte[] { 1, 2, 3 }, Byte[].class);
    assertEquivalent("[1, null, 3]", new Byte[] { 1, null, 3 }, Byte[].class);
    assertException("[1, 214321, 3]", Byte[].class, JSDataException.class, "Can't treat 214321 as byte");
    assertEquivalent("[]", new Byte[] { }, Byte[].class);
    assertEquivalent("null", null, Byte[].class);
    assertEquivalent("undefined", null, Byte[].class);
    assertException("false", Byte[].class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void intArray() {
    assertEquivalent("[1, 2, 3]", new int[] { 1, 2, 3 }, int[].class);
    assertException("[1, null, 3]", int[].class, JSDataException.class, "Can't pickle the JSValue");
    assertException("[1, 2.1, 3]", int[].class, JSDataException.class, "Can't treat 2.1 as int");
    assertEquivalent("[]", new int[] { }, int[].class);
    assertEquivalent("null", null, int[].class);
    assertEquivalent("undefined", null, int[].class);
    assertException("false", int[].class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("[1, 2, 3]", new Integer[] { 1, 2, 3 }, Integer[].class);
    assertEquivalent("[1, null, 3]", new Integer[] { 1, null, 3 }, Integer[].class);
    assertException("[1, 2.1, 3]", Integer[].class, JSDataException.class, "Can't treat 2.1 as int");
    assertEquivalent("[]", new Integer[] { }, Integer[].class);
    assertEquivalent("null", null, Integer[].class);
    assertEquivalent("undefined", null, Integer[].class);
    assertException("false", Integer[].class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void floatArray() {
    assertEquivalent("[1.1, 2, 3.5]", new float[] { 1.1f, 2f, 3.5f }, float[].class);
    assertException("[1.1, null, 3.5]", float[].class, JSDataException.class, "Can't pickle the JSValue");
    assertException("[1.1, false, 3.5]", float[].class, JSDataException.class, "Can't pickle the JSValue");
    assertEquivalent("[]", new float[] { }, float[].class);
    assertEquivalent("null", null, float[].class);
    assertEquivalent("undefined", null, float[].class);
    assertException("false", float[].class, JSDataException.class, "Can't pickle the JSValue");

    assertEquivalent("[1.1, 2, 3.5]", new Float[] { 1.1f, 2f, 3.5f }, Float[].class);
    assertEquivalent("[1.1, null, 3.5]", new Float[] { 1.1f, null, 3.5f }, Float[].class);
    assertException("[1.1, false, 3.5]", Float[].class, JSDataException.class, "Can't pickle the JSValue");
    assertEquivalent("[]", new Float[] { }, Float[].class);
    assertEquivalent("null", null, Float[].class);
    assertEquivalent("undefined", null, Float[].class);
    assertException("false", Float[].class, JSDataException.class, "Can't pickle the JSValue");
  }

  @Test
  public void stringArray() {
    assertEquivalent("['str', 'ing']", new String[] { "str", "ing" }, String[].class);
    assertEquivalent("['str', null, 'ing']", new String[] { "str", null, "ing" }, String[].class);
    assertException("['str', false, 'ing']", String[].class, JSDataException.class, "Can't pickle the JSValue");
    assertEquivalent("[]", new String[] { }, String[].class);
    assertEquivalent("null", null, String[].class);
    assertEquivalent("undefined", null, String[].class);
    assertException("false", String[].class, JSDataException.class, "Can't pickle the JSValue");
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
    assertException("[false]", String[][].class, JSDataException.class, "Can't pickle the JSValue");
    assertException("[[false]]", String[][].class, JSDataException.class, "Can't pickle the JSValue");
    assertException("[[[false]]]", String[][].class, JSDataException.class, "Can't pickle the JSValue");
  }
}
