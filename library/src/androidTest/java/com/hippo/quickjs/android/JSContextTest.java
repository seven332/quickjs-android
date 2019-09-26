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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.junit.Assert.assertEquals;

public class JSContextTest {

  private interface Tester<T> {
    void assertEquals(T excepted, T actual);
  }

  private static class SetData<T> {
    String name;
    GenericType<T> type;
    T value;
    Tester<T> tester;
    SetData(String name, GenericType<T> type, T value, Tester<T> tester) {
      this.name = name;
      this.type = type;
      this.value = value;
      this.tester = tester;
    }
  }

  private static SetData<?>[] ALL_SET_DATA = {
      new SetData<>("boolean true", new GenericType<>(boolean.class), true, Assert::assertEquals),
      new SetData<>("byte -21", new GenericType<>(byte.class), (byte) -21, Assert::assertEquals),
      new SetData<>("char z", new GenericType<>(char.class), 'z', Assert::assertEquals),
      new SetData<>("short 321", new GenericType<>(short.class), (short) 321, Assert::assertEquals),
      new SetData<>("int 32432562", new GenericType<>(int.class), 32432562, Assert::assertEquals),
      new SetData<>("long 3243256234234L", new GenericType<>(long.class), 3243256234234L, Assert::assertEquals),
      new SetData<>("float 324.5436f", new GenericType<>(float.class), 324.5436f, Assert::assertEquals),
      new SetData<>("double 56245.6234", new GenericType<>(double.class), 56245.6234, Assert::assertEquals),
      new SetData<>("NonNull String strings", new GenericType<>(Types.nonNullOf(String.class)), "strings", Assert::assertEquals),

      new SetData<>("Boolean true", new GenericType<>(Boolean.class), true, Assert::assertEquals),
      new SetData<>("Boolean null", new GenericType<>(Boolean.class), null, Assert::assertEquals),

      new SetData<>("String array", new GenericType<>(String[].class), new String[] { "str", null, "ing" }, Assert::assertArrayEquals),
      new SetData<String[]>("String array null", new GenericType<>(String[].class), null, Assert::assertArrayEquals),

      new SetData<>(
          "String array array",
          new GenericType<>(String[][].class),
          new String[][] { new String[]{"str", "ing"}, new String[]{"st", "ri", "ng"} },
          Assert::assertArrayEquals
      ),
  };

  private  <T> void testSet(JSContext context, SetData<T> data) {
    context.set("_set_data_", data.type, data.value);
    data.tester.assertEquals(data.value, context.evaluate("_set_data_", "test.js", data.type));
  }

  @Test
  public void set() {
    try (QuickJS quickJS = new QuickJS.Builder().build()) {
      try (JSRuntime runtime = quickJS.createJSRuntime()) {
        try (JSContext context = runtime.createJSContext()) {
          for (SetData<?> data : ALL_SET_DATA) {
            try {
              testSet(context, data);
            } catch (Throwable e) {
              throw new AssertionError("<" + data.name + "> failed", e);
            }
          }
        }
      }
    }
  }

  @Ignore("There is no guarantee that this test will pass")
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
  public void throwException() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertException(
            JSEvaluationException.class,
            "Throw: 1\n",
            () -> context.evaluate("throw 1", "unknown.js")
        );
      }
    }
  }

  @Test
  public void throwExceptionWithReturn() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertException(
            JSEvaluationException.class,
            "Throw: 1\n",
            () -> context.evaluate("throw 1", "unknown.js", int.class)
        );
      }
    }
  }
}
