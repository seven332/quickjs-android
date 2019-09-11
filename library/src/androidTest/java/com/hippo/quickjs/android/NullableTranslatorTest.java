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

public class NullableTranslatorTest {

  private <T> void assertEquivalent(String script, T[] except, GenericType<T[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertArrayEquals(except, context.evaluate(script, "test.js", type));
      }
    }
  }

  private <T> void assertException(String script, String message, GenericType<T[]> type) {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        try {
          context.evaluate(script, "test.js", type);
          fail();
        } catch (JSDataException e) {
          assertEquals(message, e.getMessage());
        }
      }
    }
  }

  @Test
  public void nonNullElement() {
    GenericType<String[]> type = new GenericType<>(
        Types.nonNullOf(
            Types.arrayOf(
                Types.nonNullOf(
                    String.class
                )
            )
        )
    );

    assertEquivalent("['str', 'ing']", new String[] { "str", "ing" }, type);

    assertException(
        "null",
        "Can't pickle the JSValue",
        type
    );

    assertException(
        "['str', null, 'ing']",
        "Can't pickle the JSValue",
        type
    );
  }
}
