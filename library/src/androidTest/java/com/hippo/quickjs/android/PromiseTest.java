/*
 * Copyright 2020 Hippo Seven
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

import static org.assertj.core.api.Assertions.assertThat;

public class PromiseTest {

  private static String sLog;

  public static void log(String log) {
    sLog = log;
  }

  @Test
  public void testPromise() throws NoSuchMethodException {
    QuickJS quickJS = new QuickJS.Builder().build();

    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {

        Method method = Method.create(Void.class, PromiseTest.class.getMethod("log", String.class));
        context.getGlobalObject().setProperty("log", context.createJSFunctionS(PromiseTest.class, method));

        context.evaluate("log('before')\n" +
          "Promise.resolve()\n" +
          "  .then(() => { log('in promise 1') })\n" +
          "  .then(() => { log('in promise 2') })", "test.js");

        assertThat(sLog).isEqualTo("before");

        assertThat(context.executePendingJob()).isEqualTo(true);
        assertThat(sLog).isEqualTo("in promise 1");

        assertThat(context.executePendingJob()).isEqualTo(true);
        assertThat(sLog).isEqualTo("in promise 2");

        assertThat(context.executePendingJob()).isEqualTo(false);
      }
    }
  }

  @Test
  public void testDumbPromise() {
    QuickJS quickJS = new QuickJS.Builder().build();

    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        context.evaluate("new Promise((resolve, reject) => {}).then(() => {})\n", "test.js");
        assertThat(context.executePendingJob()).isEqualTo(false);
      }
    }
  }
}
