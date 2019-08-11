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

public class JSContextTest {

  @Test
  public void testJSValueGC() {
    QuickJS quickJS = new QuickJS.Builder().build();

    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        int jsValueCount = 3;

        assertEquals(0, context.getNotRemovedJSValueCount());

        for (int i = 0; i < jsValueCount; i++) {
          context.evaluate("1", "unknown.js", int.class);
        }

        assertEquals(jsValueCount, context.getNotRemovedJSValueCount());

        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();

        context.evaluate("1", "unknown.js", int.class);

        assertEquals(1, context.getNotRemovedJSValueCount());
      }
    }
  }

  @Test
  public void getGlobalObject() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        context.evaluate("a = 1", "unknown.js", int.class);
        assertEquals(1, context.getGlobalObject().getProperty("a").cast(JSNumber.class).getInt());

        context.getGlobalObject().setProperty("b", context.createJSString("string"));
        assertEquals("string", context.evaluate("b", "unknown.js", String.class));
      }
    }
  }
}
