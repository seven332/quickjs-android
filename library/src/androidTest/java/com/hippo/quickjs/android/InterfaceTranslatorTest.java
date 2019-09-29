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

import androidx.annotation.Nullable;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.junit.Assert.assertEquals;

public class InterfaceTranslatorTest {

  @Test
  public void JSValueToJavaInterface() {
    try (QuickJS quickJS = new QuickJS.Builder().addTranslator(Types.nonNullOf(NumberHolder.class), new NumberHolderTranslator()).build()) {
      try (JSRuntime runtime = quickJS.createJSRuntime()) {
        try (JSContext context = runtime.createJSContext()) {
          Calculator calculator = context.evaluate("" +
              "a = {\n" +
              "  plus: function(a, b) { return a + b },\n" +
              "  minus: function(a, b) { return a - b },\n" +
              "  multiplies: function(a, b) { return a * b },\n" +
              "  divides: function(a, b) { return a / b },\n" +
              "  plusWrap: function(a, b) { return { number: a.number + b.number } },\n" +
              "  minusWrap: function(a, b) { return { number: a.number - b.number } },\n" +
              "  multipliesWrap: function(a, b) { return { number: a.number * b.number } },\n" +
              "  dividesWrap: function(a, b) { return { number: a.number / b.number } },\n" +
              "  noop: function() { }\n" +
              "}", "test.js", Calculator.class);

          double a = 324.324;
          double b = -0.5432;
          NumberHolder aa = new NumberHolder(a);
          NumberHolder bb = new NumberHolder(b);

          assertEquals(a + b, calculator.plus(a, b), 0.0);
          assertEquals(a - b, calculator.minus(a, b), 0.0);
          assertEquals(a * b, calculator.multiplies(a, b), 0.0);
          assertEquals(a / b, calculator.divides(a, b), 0.0);

          assertEquals(new NumberHolder(a + b), calculator.plusWrap(aa, bb));
          assertEquals(new NumberHolder(a - b), calculator.minusWrap(aa, bb));
          assertEquals(new NumberHolder(a * b), calculator.multipliesWrap(aa, bb));
          assertEquals(new NumberHolder(a / b), calculator.dividesWrap(aa, bb));

          calculator.noop();
        }
      }
    }
  }

  interface Calculator {
    double plus(double a, double b);
    double minus(double a, double b);
    double multiplies(double a, double b);
    double divides(double a, double b);
    NumberHolder plusWrap(NumberHolder a, NumberHolder b);
    NumberHolder minusWrap(NumberHolder a, NumberHolder b);
    NumberHolder multipliesWrap(NumberHolder a, NumberHolder b);
    NumberHolder dividesWrap(NumberHolder a, NumberHolder b);
    void noop();
  }

  static class NumberHolder {
    double number;
    NumberHolder(double number) {
      this.number = number;
    }
    @Override
    public int hashCode() {
      return Double.hashCode(number);
    }
    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof NumberHolder && ((NumberHolder) obj).number == number;
    }
  }

  static class NumberHolderTranslator extends Translator<NumberHolder> {

    private static byte[] PICKLE_COMMAND = new byte[] {
        PICKLE_FLAG_OPT_PUSH,
        PICKLE_FLAG_PROP_STR,
        7,
        0,
        0,
        0,
        'n',
        'u',
        'm',
        'b',
        'e',
        'r',
        0,
        PICKLE_FLAG_TYPE_NUMBER,
        PICKLE_FLAG_OPT_POP,
    };

    private static byte[] UNPICKLE_COMMAND = new byte[] {
        UNPICKLE_FLAG_OPT_PUSH,
        UNPICKLE_FLAG_TYPE_DOUBLE,
        UNPICKLE_FLAG_PROP_STR,
        7,
        0,
        0,
        0,
        'n',
        'u',
        'm',
        'b',
        'e',
        'r',
        0,
        PICKLE_FLAG_OPT_POP,
    };

    NumberHolderTranslator() {
      super(PICKLE_COMMAND, UNPICKLE_COMMAND);
    }

    @Override
    protected NumberHolder unpickle(JSContext context, BitSource source) {
      return new NumberHolder(source.readDouble());
    }

    @Override
    protected void pickle(JSContext context, NumberHolder value, BitSink sink) {
      sink.writeDouble(value.number);
    }
  }

  @Test
  public void closeWithoutClose() throws IOException {
    try (QuickJS quickJS = new QuickJS.Builder().build()) {
      try (JSRuntime runtime = quickJS.createJSRuntime()) {
        try (JSContext context = runtime.createJSContext()) {

          context.evaluate("number = 0", "test.js");

          WithoutClose obj1 = context.evaluate("" +
              "a = {\n" +
              "  return1: function() { return 1 },\n" +
              "  close: function() { number = 1 }\n" +
              "}", "test.js", WithoutClose.class);
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          assertEquals(1, obj1.return1());
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          ((Closeable) obj1).close();
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          ((Closeable) obj1).close();
          assertException(IllegalStateException.class, "The JSValue is closed", obj1::return1);

          context.evaluate("number = 0", "test.js");

          WithoutClose obj2 = context.evaluate("" +
              "a = {\n" +
              "  return1: function() { return 1 }\n" +
              "}", "test.js", WithoutClose.class);
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          assertEquals(1, obj2.return1());
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          ((Closeable) obj2).close();
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          ((Closeable) obj2).close();
          assertException(IllegalStateException.class, "The JSValue is closed", obj2::return1);
        }
      }
    }
  }

  @Test
  public void closeWithClose() throws IOException {
    try (QuickJS quickJS = new QuickJS.Builder().build()) {
      try (JSRuntime runtime = quickJS.createJSRuntime()) {
        try (JSContext context = runtime.createJSContext()) {

          context.evaluate("number = 0", "test.js");

          WithClose obj1 = context.evaluate("" +
              "a = {\n" +
              "  return1: function() { return 1 },\n" +
              "  close: function() { number = 1 }\n" +
              "}", "test.js", WithClose.class);
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          assertEquals(1, obj1.return1());
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          obj1.close();
          assertEquals(1, (int) context.evaluate("number", "test.js", int.class));
          obj1.close();
          assertException(IllegalStateException.class, "The JSValue is closed", obj1::return1);

          context.evaluate("number = 0", "test.js");

          WithClose obj2 = context.evaluate("" +
              "a = {\n" +
              "  return1: function() { return 1 }\n" +
              "}", "test.js", WithClose.class);
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          assertEquals(1, obj2.return1());
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          obj2.close();
          assertEquals(0, (int) context.evaluate("number", "test.js", int.class));
          obj2.close();
          assertException(IllegalStateException.class, "The JSValue is closed", obj2::return1);
        }
      }
    }
  }

  public interface WithoutClose {
    int return1();
  }
  public interface WithClose extends Closeable {
    int return1();
  }
}
