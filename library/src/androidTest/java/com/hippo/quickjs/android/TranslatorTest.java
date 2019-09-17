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

public class TranslatorTest {

  @Test
  public void testEntry() {
    QuickJS quickJS = new QuickJS.Builder().addTranslator(TestEntry.class, new TestTranslator()).build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {

        TestEntry entry = context.evaluate("" +
            "a = {};" +
            "a.strings = ['str', 'ing'];" +
            "a[32] = 432.123;" +
            "a;" +
            "", "test.js", TestEntry.class);

        assertArrayEquals(new String[] { "str", "ing" }, entry.strings);
        assertEquals(432.123, entry.d, 0.0);
      }
    }
  }

  private static class TestTranslator extends Translator<TestEntry> {

    private static final byte[] PICKLE_COMMAND = {
        PICKLE_FLAG_OPT_PUSH,
        PICKLE_FLAG_PROP_STR,
        8,
        0,
        0,
        0,
        's', // strings
        't',
        'r',
        'i',
        'n',
        'g',
        's',
        0,
        PICKLE_FLAG_TYPE_ARRAY,
        1,
        0,
        0,
        0,
        PICKLE_FLAG_TYPE_STRING,
        PICKLE_FLAG_PROP_INT,
        32,
        0,
        0,
        0,
        PICKLE_FLAG_TYPE_NUMBER,
        PICKLE_FLAG_OPT_POP,
    };

    private static final byte[] UNPICKLE_COMMAND = { };

    TestTranslator() {
      super(PICKLE_COMMAND, UNPICKLE_COMMAND);
    }

    @Override
    protected TestEntry unpickle(BitSource source) {
      int length = source.readArrayLength();
      String[] strings = new String[length];
      for (int i = 0; i < length; i++) {
        strings[i] = source.readString();
      }
      double d = source.readDouble();

      TestEntry entry = new TestEntry();
      entry.strings = strings;
      entry.d = d;

      return entry;
    }

    @Override
    protected void pickle(TestEntry value, BitSink sink) {
      throw new IllegalStateException("TODO");
    }
  }

  private static class TestEntry {
    String[] strings;
    double d;
  }
}
