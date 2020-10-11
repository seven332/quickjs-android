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

import org.junit.Ignore;
import org.junit.Test;

import static com.hippo.quickjs.android.Utils.assertException;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuickJSTest {
  @Test
  public void createRuntime() {
    long runtime = QuickJS.createRuntime();
    assertNotEquals(0, runtime);
    QuickJS.destroyRuntime(runtime);
  }

  @Test
  public void destroyRuntime_0() {
    assertException(
        IllegalStateException.class,
        "Null JSRuntime",
        () -> QuickJS.destroyRuntime(0)
    );
  }

  private void withRuntime(WithRuntimeBlock block) {
    long runtime = QuickJS.createRuntime();
    assertNotEquals(0, runtime);
    try {
      block.run(runtime);
    } finally {
      QuickJS.destroyRuntime(runtime);
    }
  }

  private interface WithRuntimeBlock {
    void run(long runtime);
  }

  @Ignore("It causes accessing null pointer in C")
  @Test
  public void testOutOfMemory() {
    for (int i = 0; i < 50000; i++) {
      int mallocLimit = i;
      withRuntime(runtime -> {
        QuickJS.setRuntimeMallocLimit(runtime, mallocLimit);
        assertException(
            IllegalStateException.class,
            "Out of memory",
            () -> QuickJS.createContext(runtime)
        );
      });
    }
  }

  @Test
  public void setRuntimeInterruptHandler_interruptedAtOnce() {
    withRuntimeContext((runtime, context) -> {
      QuickJS.setRuntimeInterruptHandler(runtime, () -> true);

      withScript(context, "i = 0", value -> {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
        JSException jsException = QuickJS.getException(context);
        assertTrue(jsException.isError());
        assertEquals("InternalError: interrupted", jsException.getException());
        assertEquals("", jsException.getStack());
      });
    });
  }

  @Test
  public void setRuntimeInterruptHandler_10000times() {
    withRuntimeContext((runtime, context) -> {
      QuickJS.setRuntimeInterruptHandler(runtime, () -> true);

      withScript(context, "i = 0", value -> {});

      withScript(context, "i=0;while(true){i++;}", value -> {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
        JSException jsException = QuickJS.getException(context);
        assertTrue(jsException.isError());
        assertEquals("InternalError: interrupted", jsException.getException());
        assertEquals("    at <eval> (source.js)\n", jsException.getStack());
      });

      withScript(context, "i", value -> {
        assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value));
        assertEquals(9999, QuickJS.getValueInt(value));
      });
    });
  }

  @Test
  public void setRuntimeInterruptHandler_nonNullToNull_noInterrupted() {
    withRuntimeContext((runtime, context) -> {
      QuickJS.setRuntimeInterruptHandler(runtime, () -> true);
      QuickJS.setRuntimeInterruptHandler(runtime, null);

      withScript(context, "i = 100000;while(i!=0){i--;}", value -> {
        assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value));
        assertEquals(1, QuickJS.getValueInt(value));
      });
    });
  }

  @Test
  public void setRuntimeInterruptHandler_nullToNull_noInterrupted() {
    withRuntimeContext((runtime, context) -> {
      QuickJS.setRuntimeInterruptHandler(runtime, null);
      QuickJS.setRuntimeInterruptHandler(runtime, null);

      withScript(context, "i = 100000;while(i!=0){i--;}", value -> {
        assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value));
        assertEquals(1, QuickJS.getValueInt(value));
      });
    });
  }

  @Test
  public void createContext() {
    withRuntime(runtime -> {
      long context = QuickJS.createContext(runtime);
      assertNotEquals(0, context);
      QuickJS.destroyContext(context);
    });
  }

  @Test
  public void destroyContext() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.destroyContext(0)
    );
  }

  private void withRuntimeContext(WithRuntimeContextBlock block) {
    withRuntime(runtime -> {
      long context = QuickJS.createContext(runtime);
      assertNotEquals(0, context);
      try {
        block.run(runtime, context);
      } finally {
        QuickJS.destroyContext(context);
      }
    });
  }

  private interface WithRuntimeContextBlock {
    void run(long runtime, long context);
  }

  private void withValue(long context, long value, WithValueBlock block) {
    assertNotEquals(0, value);
    try {
      block.run(value);
    } finally {
      QuickJS.destroyValue(context, value);
    }
  }

  private interface WithValueBlock {
    void run(long value);
  }

  @Test
  public void createValueUndefined() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueUndefined(context), value ->
        assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(value))
      )
    );
  }

  @Test
  public void createValueUndefined_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueUndefined(0)
    );
  }

  @Test
  public void createValueNull() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueNull(context), value ->
        assertEquals(JSContext.TYPE_NULL, QuickJS.getValueTag(value))
      )
    );
  }

  @Test
  public void createValueNull_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueNull(0)
    );
  }

  @Test
  public void createValueBoolean() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueBoolean(context, true), value ->
        assertTrue(QuickJS.getValueBoolean(value))
      )
    );
  }

  @Test
  public void createValueBoolean_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueBoolean(0, true)
    );
  }

  @Test
  public void createValueInt() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueInt(context, 32), value ->
        assertEquals(32, QuickJS.getValueInt(value))
      )
    );
  }

  @Test
  public void createValueInt_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueInt(0, 32)
    );
  }

  @Test
  public void createValueFloat64() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueFloat64(context, 1.11), value ->
        assertEquals(1.11, QuickJS.getValueFloat64(value), 0.0)
      )
    );
  }

  @Test
  public void createValueFloat64_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueFloat64(0, 1.11)
    );
  }

  @Test
  public void createValueString() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueString(context, "string"), value ->
        assertEquals("string", QuickJS.getValueString(context, value))
      )
    );
  }

  @Test
  public void createValueString_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueString(0, null)
    );
  }

  @Test
  public void createValueString_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null value",
      () -> QuickJS.createValueString(context, null)
    ));
  }

  @Test
  public void createValueObject() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueObject(context), value ->
        assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(value))
      )
    );
  }

  @Test
  public void createValueObject_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueObject(0)
    );
  }

  @Test
  public void createValueArray() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueArray(context), value ->
        assertTrue(QuickJS.isValueArray(context, value))
      )
    );
  }

  @Test
  public void createValueArray_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.createValueArray(0)
    );
  }

  @Test
  public void getValueTag_nullValue_error() {
    assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueTag(0)
    );
  }

  private void withScript(long context, String script, WithScriptBlock block) {
    long value = QuickJS.evaluate(context, script, "source.js", 0);
    assertNotEquals(0, value);
    try {
      block.run(value);
    } finally {
      QuickJS.destroyValue(context, value);
    }
  }

  private interface WithScriptBlock {
    void run(long value);
  }

  private void withRuntimeContextScript(String script, WithRuntimeContextScriptBlock block) {
    withRuntimeContext((runtime, context) ->
        withScript(context, script, value ->
            block.run(runtime, context, value)
        )
    );
  }

  private interface WithRuntimeContextScriptBlock {
    void run(long runtime, long context, long value);
  }

  @Test
  public void isValueArray_array_isArray() {
    withRuntimeContextScript("[]", (runtime, context, value) -> assertTrue(QuickJS.isValueArray(context, value)));
  }

  @Test
  public void isValueArray_boolean_isNotArray() {
    withRuntimeContextScript("false", (runtime, context, value) -> assertFalse(QuickJS.isValueArray(context, value)));
  }

  @Test
  public void isValueArray_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.isValueArray(0, 0)
    );
  }

  @Test
  public void isValueArray_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.isValueArray(context, 0)
    ));
  }

  @Test
  public void isValueFunction_function_isFunction() {
    withRuntimeContextScript("b = function(){}", (runtime, context, value) -> assertTrue(QuickJS.isValueFunction(context, value)));
  }

  @Test
  public void isValueFunction_boolean_isFunction() {
    withRuntimeContextScript("false", (runtime, context, value) -> assertFalse(QuickJS.isValueFunction(context, value)));
  }

  @Test
  public void isValueFunction_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.isValueFunction(0, 0)
    );
  }

  @Test
  public void isValueFunction_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.isValueFunction(context, 0)
    ));
  }

  private void withProperty(long context, long value, int index, WithPropertyBlock block) {
    long property = QuickJS.getValueProperty(context, value, index);
    assertNotEquals(0, property);
    try {
      block.run(property);
    } finally {
      QuickJS.destroyValue(context, property);
    }
  }

  private void withProperty(long context, long value, String name, WithPropertyBlock block) {
    long property = QuickJS.getValueProperty(context, value, name);
    assertNotEquals(0, property);
    try {
      block.run(property);
    } finally {
      QuickJS.destroyValue(context, property);
    }
  }

  private interface WithPropertyBlock {
    void run(long property);
  }

  @Test
  public void getValuePropertyInt() {
    withRuntimeContextScript("[1, 'str']", (runtime, context, value) -> {
      withProperty(context, value, 0, property -> assertEquals(1, QuickJS.getValueInt(property)));
      withProperty(context, value, 1, property -> assertEquals("str", QuickJS.getValueString(context, property)));
    });
  }

  @Test
  public void getValuePropertyInt_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.getValueProperty(0, 0, 0)
    );
  }

  @Test
  public void getValuePropertyInt_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueProperty(context, 0, 0)
    ));
  }

  @Test
  public void getValuePropertyString() {
    withRuntimeContextScript("a = {a: 1, b: 'str'}", (runtime, context, value) -> {
      withProperty(context, value, "a", property -> assertEquals(1, QuickJS.getValueInt(property)));
      withProperty(context, value, "b", property -> assertEquals("str", QuickJS.getValueString(context, property)));
    });
  }

  @Test
  public void getValuePropertyString_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.getValueProperty(0, 0, null)
    );
  }

  @Test
  public void getValuePropertyString_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueProperty(context, 0, null)
    ));
  }

  @Test
  public void getValuePropertyString_nullName_error() {
    withRuntimeContextScript("a = {a: 1, b: 'str'}", (runtime, context, value) -> assertException(
      IllegalStateException.class,
      "Null name",
      () -> QuickJS.getValueProperty(context, value, null)
    ));
  }

  @Test
  public void setValuePropertyInt() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueObject(context), value ->
        withValue(context, QuickJS.createValueObject(context), property -> {
          withProperty(context, value, 1, p -> assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(p)));
          assertTrue(QuickJS.setValueProperty(context, value, 1, property));
          withProperty(context, value, 1, p -> assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(p)));
        })
      )
    );
  }

  @Test
  public void setValuePropertyInt_undefinedValue_error() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueUndefined(context), value ->
        withValue(context, QuickJS.createValueObject(context), property -> {
          assertFalse(QuickJS.setValueProperty(context, value, 1, property));
          assertEquals("TypeError: cannot set property '1' of undefined\n", QuickJS.getException(context).toString());
        })
      )
    );
  }

  @Test
  public void setValuePropertyInt_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.setValueProperty(0, 0, 0, 0)
    );
  }

  @Test
  public void setValuePropertyInt_nullValue_error() {
    withRuntimeContext((runtime, context) ->
      assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.setValueProperty(context, 0, 0, 0)
      )
    );
  }

  @Test
  public void setValuePropertyInt_nullProperty_error() {
    withRuntimeContextScript("a = {}", (runtime, context, value) ->
      assertException(
        IllegalStateException.class,
        "Null property",
        () -> QuickJS.setValueProperty(context, value, 0, 0)
      )
    );
  }

  @Test
  public void setValuePropertyString() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueObject(context), value ->
        withValue(context, QuickJS.createValueObject(context), property -> {
          withProperty(context, value, "prop", p -> assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(p)));
          assertTrue(QuickJS.setValueProperty(context, value, "prop", property));
          withProperty(context, value, "prop", p -> assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(p)));
        })
      )
    );
  }

  @Test
  public void setValuePropertyString_undefinedValue_error() {
    withRuntimeContext((runtime, context) ->
      withValue(context, QuickJS.createValueUndefined(context), value ->
        withValue(context, QuickJS.createValueObject(context), property -> {
          assertFalse(QuickJS.setValueProperty(context, value, "prop", property));
          assertEquals("TypeError: cannot set property 'prop' of undefined\n", QuickJS.getException(context).toString());
        })
      )
    );
  }

  @Test
  public void setValuePropertyString_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.setValueProperty(0, 0, null, 0)
    );
  }

  @Test
  public void setValuePropertyString_nullValue_error() {
    withRuntimeContext((runtime, context) ->
      assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.setValueProperty(context, 0, null, 0)
      )
    );
  }

  @Test
  public void setValuePropertyString_nullName_error() {
    withRuntimeContextScript("a = {}", (runtime, context, value) ->
      assertException(
        IllegalStateException.class,
        "Null name",
        () -> QuickJS.setValueProperty(context, value, null, 0)
      )
    );
  }

  @Test
  public void setValuePropertyString_nullProperty_error() {
    withRuntimeContextScript("a = {}", (runtime, context, value) ->
      assertException(
        IllegalStateException.class,
        "Null property",
        () -> QuickJS.setValueProperty(context, value, "prop", 0)
      )
    );
  }

  @Test
  public void getValueBoolean_true() {
    withRuntimeContextScript("true", (runtime, context, value) -> assertTrue(QuickJS.getValueBoolean(value)));
  }

  @Test
  public void getValueBoolean_false() {
    withRuntimeContextScript("false", (runtime, context, value) -> assertFalse(QuickJS.getValueBoolean(value)));
  }

  @Test
  public void getValueBoolean_notBoolean_error() {
    withRuntimeContextScript("1", (runtime, context, value) ->
      assertException(
        JSDataException.class,
        "Invalid JSValue tag for boolean: 0",
        () -> QuickJS.getValueBoolean(value)
      )
    );
  }

  @Test
  public void getValueBoolean_nullValue_error() {
    assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueBoolean(0)
    );
  }

  @Test
  public void getValueInt() {
    withRuntimeContextScript("123", (runtime, context, value) -> assertEquals(123, QuickJS.getValueInt(value)));
  }

  @Test
  public void getValueInt_notInt_error() {
    withRuntimeContextScript("123.1", (runtime, context, value) ->
      assertException(
        JSDataException.class,
        "Invalid JSValue tag for int: 7",
        () -> QuickJS.getValueInt(value)
      )
    );
  }

  @Test
  public void getValueInt_nullValue_error() {
    assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueInt(0)
    );
  }

  @Test
  public void getValueFloat64() {
    withRuntimeContextScript("123.1", (runtime, context, value) -> assertEquals(123.1, QuickJS.getValueFloat64(value), 0.0));
  }

  @Test
  public void getValueFloat64_notFloat64_error() {
    withRuntimeContextScript("123", (runtime, context, value) ->
      assertException(
        JSDataException.class,
        "Invalid JSValue tag for float64: 0",
        () -> QuickJS.getValueFloat64(value)
      )
    );
  }

  @Test
  public void getValueFloat64_nullValue_error() {
    assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueFloat64(0)
    );
  }

  @Test
  public void getValueString() {
    withRuntimeContextScript("'string'", (runtime, context, value) -> assertEquals("string", QuickJS.getValueString(context, value)));
  }

  @Test
  public void getValueString_notString_error() {
    withRuntimeContextScript("123", (runtime, context, value) ->
      assertException(
        JSDataException.class,
        "Invalid JSValue tag for string: 0",
        () -> QuickJS.getValueString(context, value)
      )
    );
  }

  @Test
  public void getValueString_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.getValueString(0, 0)
    );
  }

  @Test
  public void getValueString_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.getValueString(context, 0)
    ));
  }

  @Test
  public void invokeValueFunction() {
    withRuntimeContextScript("f=function a(i,j){return i*j}", (runtime, context, function) ->
      withScript(context, "3", valueI ->
        withScript(context, "9", (WithScriptBlock) valueJ ->
          withValue(context, QuickJS.invokeValueFunction(context, function, 0, new long[]{valueI, valueJ}), ret ->
            assertEquals(27, QuickJS.getValueInt(ret))
          )
        )
      )
    );
  }

  @Test
  public void invokeValueFunction_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.invokeValueFunction(0, 0, 0, null)
    );
  }

  @Test
  public void invokeValueFunction_nullFunction_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null function",
      () -> QuickJS.invokeValueFunction(context, 0, 0, null)
    ));
  }

  @Test
  public void invokeValueFunction_nullArguments_error() {
    withRuntimeContextScript("f=function a(i,j){return i*j}", (runtime, context, function) -> assertException(
      IllegalStateException.class,
      "Null arguments",
      () -> QuickJS.invokeValueFunction(context, function, 0, null)
    ));
  }

  @Test
  public void destroyValue_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.destroyValue(0, 0)
    );
  }

  @Test
  public void destroyValue_nullValue_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null JSValue",
      () -> QuickJS.destroyValue(context, 0)
    ));
  }

  @Test
  public void getException_notError() {
    withRuntimeContextScript("throw 1", (runtime, context, value) -> {
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
      JSException jsException = QuickJS.getException(context);
      assertFalse(jsException.isError());
      assertEquals("1", jsException.getException());
      assertNull(jsException.getStack());
    });
  }

  @Test
  public void getException_error() {
    withRuntimeContextScript("throw new Error()", (runtime, context, value) -> {
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
      JSException jsException = QuickJS.getException(context);
      assertTrue(jsException.isError());
      assertEquals("Error", jsException.getException());
      assertEquals("    at <eval> (source.js)\n", jsException.getStack());
    });
  }

  @Test
  public void getException_stack() {
    withRuntimeContextScript("" +
        "(function() {\n" +
        "    function f1() {\n" +
        "        throw new Error()\n" +
        "    }\n" +
        "    function f2() {\n" +
        "        f1()\n" +
        "    }\n" +
        "    function f3() {\n" +
        "        f2()\n" +
        "    }\n" +
        "    f3()\n" +
        "})()", (runtime, context, value) -> {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
        JSException jsException = QuickJS.getException(context);
        assertTrue(jsException.isError());
        assertEquals("Error", jsException.getException());
        assertEquals(("" +
          "    at f1 (source.js:3)\n" +
          "    at f2 (source.js:6)\n" +
          "    at f3 (source.js:9)\n" +
          "    at <anonymous> (source.js:11)\n" +
          "    at <eval> (source.js:12)\n"), jsException.getStack()
        );
      }
    );
  }

  @Test
  public void getException_noError() {
    withRuntimeContextScript("1", (runtime, context, value) -> {
      assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value));
      JSException jsException = QuickJS.getException(context);
      assertFalse(jsException.isError());
      assertEquals("null", jsException.getException());
      assertNull(jsException.getStack());
    });
  }

  @Test
  public void getException_nullContext_null() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.getException(0)
    );
  }

  @Test
  public void evaluate_nullContext_error() {
    assertException(
      IllegalStateException.class,
      "Null JSContext",
      () -> QuickJS.evaluate(0, null, null, 0)
    );
  }

  @Test
  public void evaluate_nullSourceCode_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null source code",
      () -> QuickJS.evaluate(context, null, null, 0)
    ));
  }

  @Test
  public void evaluate_nullFileName_error() {
    withRuntimeContext((runtime, context) -> assertException(
      IllegalStateException.class,
      "Null file name",
      () -> QuickJS.evaluate(context, "", null, 0)
    ));
  }
}
