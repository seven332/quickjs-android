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

public class JSRuntimeTest {

  @Test
  public void setInterruptHandler() {
    try (QuickJS quickJS = new QuickJS.Builder().build()) {
      try (JSRuntime runtime = quickJS.createJSRuntime()) {
        try (JSContext context = runtime.createJSContext()) {

          runtime.setInterruptHandler(() -> true);

          assertException(
              JSEvaluationException.class,
              "InternalError: interrupted\n",
              () -> context.evaluate("i = 0", "test.js")
          );

          runtime.setInterruptHandler(() -> true);

          assertException(
              JSEvaluationException.class,
              "InternalError: interrupted\n    at <eval> (test.js)\n",
              () -> context.evaluate("i=0;while(true){i++;}", "test.js")
          );

          assertEquals(9998, (int) context.evaluate("i", "test.js", int.class));

          runtime.setInterruptHandler(null);

          assertEquals(1, (int) context.evaluate("i=100000;while(i!=0){i--;}", "test.js", int.class));

          runtime.setInterruptHandler(null);

          assertEquals(1, (int) context.evaluate("i=100000;while(i!=0){i--;}", "test.js", int.class));
        }
      }
    }
  }
}
