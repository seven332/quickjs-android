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

import static com.hippo.quickjs.android.Utils.assertException;
import static org.junit.Assert.assertEquals;

public class JSObjectTest {

  @Test
  public void definePropertyInt() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        JSObject jo = context.createJSObject();

        int index = 23;
        int value1 = 12123;
        int value2 = 32121;

        jo.defineProperty(index, context.createJSNumber(value1), JSObject.PROP_FLAG_WRITABLE);
        assertEquals(value1, jo.getProperty(index).cast(JSNumber.class).getInt());
        jo.setProperty(index, context.createJSNumber(value2));
        assertEquals(value2, jo.getProperty(index).cast(JSNumber.class).getInt());

        jo.defineProperty(index, context.createJSNumber(value1), 0);
        assertEquals(value1, jo.getProperty(index).cast(JSNumber.class).getInt());
        assertException(
            JSEvaluationException.class,
            "TypeError: 23 is read-only\n",
            () -> jo.setProperty(index, context.createJSNumber(value2))
        );
        assertEquals(value1, jo.getProperty(index).cast(JSNumber.class).getInt());
      }
    }
  }

  @Test
  public void definePropertyString() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        JSObject jo = context.createJSObject();

        String name = "name";
        int value1 = 12123;
        int value2 = 32121;

        jo.defineProperty(name, context.createJSNumber(value1), JSObject.PROP_FLAG_WRITABLE);
        assertEquals(value1, jo.getProperty(name).cast(JSNumber.class).getInt());
        jo.setProperty(name, context.createJSNumber(value2));
        assertEquals(value2, jo.getProperty(name).cast(JSNumber.class).getInt());

        jo.defineProperty(name, context.createJSNumber(value1), 0);
        assertEquals(value1, jo.getProperty(name).cast(JSNumber.class).getInt());
        assertException(
            JSEvaluationException.class,
            "TypeError: name is read-only\n",
            () -> jo.setProperty(name, context.createJSNumber(value2))
        );
        assertEquals(value1, jo.getProperty(name).cast(JSNumber.class).getInt());
      }
    }
  }
}
