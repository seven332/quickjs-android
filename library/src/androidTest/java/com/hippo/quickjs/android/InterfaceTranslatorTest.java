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

public class InterfaceTranslatorTest {

  @Test
  public void JSValueToJavaInterface() {
    try (QuickJS quickJS = new QuickJS.Builder().build()) {
      try (JSRuntime runtime = quickJS.createJSRuntime()) {
        try (JSContext context = runtime.createJSContext()) {
          Calculator calculator = context.evaluate("" +
              "a = {\n" +
              "  plus: function(a, b) { return a + b },\n" +
              "  minus: function(a, b) { return a - b },\n" +
              "  multiplies: function(a, b) { return a * b },\n" +
              "  divides: function(a, b) { return a / b },\n" +
              "  noop: function() { }\n" +
              "}", "test.js", Calculator.class);
        }
      }
    }
  }

  interface Calculator {
    double plus(double a, double b);
    double minus(double a, double b);
    double multiplies(double a, double b);
    double divides(double a, double b);
    void noop();
  }
}
