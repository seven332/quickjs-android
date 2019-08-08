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
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class QuickJSTest {

  @Test
  public void testCreateRuntime() {
    long runtime = QuickJS.createRuntime();
    assertNotEquals(0, runtime);
    QuickJS.destroyRuntime(runtime);
  }

  @Test
  public void testDestroyRuntime() {
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

  @Test
  public void testCreateContext() {
    withRuntime(runtime -> {
      long context = QuickJS.createContext(runtime);
      assertNotEquals(0, context);
      QuickJS.destroyContext(context);
    });
  }

  @Test
  public void testDestroyContext() {
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
  public void testCreateValueUndefined() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueUndefined(context), value ->
            assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueUndefined(0)
    );
  }

  @Test
  public void testCreateValueNull() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueNull(context), value ->
            assertEquals(JSContext.TYPE_NULL, QuickJS.getValueTag(value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueNull(0)
    );
  }

  @Test
  public void testCreateValueBoolean() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueBoolean(context, true), value ->
            assertTrue(QuickJS.getValueBoolean(value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueBoolean(0, true)
    );
  }

  @Test
  public void testCreateValueInt() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueInt(context, 32), value ->
            assertEquals(32, QuickJS.getValueInt(value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueInt(0, 32)
    );
  }

  @Test
  public void testCreateValueFloat64() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueFloat64(context, 1.11), value ->
            assertEquals(1.11, QuickJS.getValueFloat64(value), 0.0)
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueFloat64(0, 1.11)
    );
  }

  @Test
  public void testCreateValueString() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueString(context, "string"), value ->
            assertEquals("string", QuickJS.getValueString(context, value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueString(0, null)
    );

    assertException(
        IllegalStateException.class,
        "Null value",
        () -> QuickJS.createValueString(1, null)
    );
  }

  @Test
  public void testCreateValueObject() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueObject(context), value ->
            assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueObject(0)
    );
  }

  @Test
  public void testCreateValueArray() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueArray(context), value ->
            assertTrue(QuickJS.isValueArray(context, value))
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.createValueArray(0)
    );
  }

  @Test
  public void testGetValueTag() {
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
  public void testIsValueArray() {
    withRuntimeContextScript("[]", (runtime, context, value) -> assertTrue(QuickJS.isValueArray(context, value)));

    withRuntimeContextScript("false", (runtime, context, value) -> assertFalse(QuickJS.isValueArray(context, value)));

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.isValueArray(0, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.isValueArray(1, 0)
    );
  }

  @Test
  public void testIsValueFunction() {
    withRuntimeContextScript("b = function(){}", (runtime, context, value) -> assertTrue(QuickJS.isValueFunction(context, value)));

    withRuntimeContextScript("false", (runtime, context, value) -> assertFalse(QuickJS.isValueFunction(context, value)));

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.isValueFunction(0, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.isValueFunction(1, 0)
    );
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
  public void testGetValuePropertyInt() {
    withRuntimeContextScript("[1, 'str']", (runtime, context, value) -> {
      withProperty(context, value, 0, property -> assertEquals(1, QuickJS.getValueInt(property)));
      withProperty(context, value, 1, property -> assertEquals("str", QuickJS.getValueString(context, property)));
    });

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.getValueProperty(0, 0, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.getValueProperty(1, 0, 0)
    );
  }

  @Test
  public void testGetValuePropertyString() {
    withRuntimeContextScript("a = {a: 1, b: 'str'}", (runtime, context, value) -> {
      withProperty(context, value, "a", property -> assertEquals(1, QuickJS.getValueInt(property)));
      withProperty(context, value, "b", property -> assertEquals("str", QuickJS.getValueString(context, property)));
    });

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.getValueProperty(0, 0, null)
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.getValueProperty(1, 0, null)
    );

    assertException(
        IllegalStateException.class,
        "Null name",
        () -> QuickJS.getValueProperty(1, 1, null)
    );
  }

  @Test
  public void testSetValuePropertyInt() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueObject(context), value ->
            withValue(context, QuickJS.createValueObject(context), property -> {
              withProperty(context, value, 1, p -> assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(p)));
              assertTrue(QuickJS.setValueProperty(context, value, 1, property));
              withProperty(context, value, 1, p -> assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(p)));
            })
        )
    );

    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueUndefined(context), value ->
            withValue(context, QuickJS.createValueObject(context), property -> {
              assertFalse(QuickJS.setValueProperty(context, value, 1, property));
              assertEquals("TypeError: value has no property\n", QuickJS.getException(context).toString());
            })
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null JSContext",
            () -> QuickJS.setValueProperty(0, 0, 0, 0)
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null JSValue",
            () -> QuickJS.setValueProperty(1, 0, 0, 0)
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null property",
            () -> QuickJS.setValueProperty(1, 1, 0, 0)
        )
    );
  }


  @Test
  public void testSetValuePropertyString() {
    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueObject(context), value ->
            withValue(context, QuickJS.createValueObject(context), property -> {
              withProperty(context, value, "prop", p -> assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(p)));
              assertTrue(QuickJS.setValueProperty(context, value, "prop", property));
              withProperty(context, value, "prop", p -> assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(p)));
            })
        )
    );

    withRuntimeContext((runtime, context) ->
        withValue(context, QuickJS.createValueUndefined(context), value ->
            withValue(context, QuickJS.createValueObject(context), property -> {
              assertFalse(QuickJS.setValueProperty(context, value, "prop", property));
              assertEquals("TypeError: value has no property\n", QuickJS.getException(context).toString());
            })
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null JSContext",
            () -> QuickJS.setValueProperty(0, 0, null, 0)
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null JSValue",
            () -> QuickJS.setValueProperty(1, 0, null, 0)
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null name",
            () -> QuickJS.setValueProperty(1, 1, null, 0)
        )
    );

    withRuntimeContext((runtime, context) ->
        assertException(
            IllegalStateException.class,
            "Null property",
            () -> QuickJS.setValueProperty(1, 1, "prop", 0)
        )
    );
  }

  @Test
  public void testGetValueBoolean() {
    withRuntimeContextScript("true", (runtime, context, value) -> assertTrue(QuickJS.getValueBoolean(value)));

    withRuntimeContextScript("false", (runtime, context, value) -> assertFalse(QuickJS.getValueBoolean(value)));

    withRuntimeContextScript("1", (runtime, context, value) ->
        assertException(
            JSDataException.class,
            "Invalid JSValue tag for boolean: 0",
            () -> QuickJS.getValueBoolean(value)
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.getValueBoolean(0)
    );
  }

  @Test
  public void testGetValueInt() {
    withRuntimeContextScript("123", (runtime, context, value) -> assertEquals(123, QuickJS.getValueInt(value)));

    withRuntimeContextScript("123.1", (runtime, context, value) ->
        assertException(
            JSDataException.class,
            "Invalid JSValue tag for int: 7",
            () -> QuickJS.getValueInt(value)
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.getValueInt(0)
    );
  }

  @Test
  public void testGetValueFloat64() {
    withRuntimeContextScript("123.1", (runtime, context, value) -> assertEquals(123.1, QuickJS.getValueFloat64(value), 0.0));

    withRuntimeContextScript("123", (runtime, context, value) ->
        assertException(
            JSDataException.class,
            "Invalid JSValue tag for float64: 0",
            () -> QuickJS.getValueFloat64(value)
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.getValueFloat64(0)
    );
  }

  @Test
  public void testGetValueString() {
    withRuntimeContextScript("'string'", (runtime, context, value) -> assertEquals("string", QuickJS.getValueString(context, value)));

    withRuntimeContextScript("123", (runtime, context, value) ->
        assertException(
            JSDataException.class,
            "Invalid JSValue tag for string: 0",
            () -> QuickJS.getValueString(context, value)
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.getValueString(0, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.getValueString(1, 0)
    );
  }

  @Test
  public void testCallValueFunction() {
    withRuntimeContextScript("f=function a(i,j){return i*j}", (runtime, context, function) ->
        withScript(context, "3", valueI ->
            withScript(context, "9", (WithScriptBlock) valueJ ->
                withValue(context, QuickJS.invokeValueFunction(context, function, 0, new long[]{valueI, valueJ}), ret ->
                    assertEquals(27, QuickJS.getValueInt(ret))
                )
            )
        )
    );

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.invokeValueFunction(0, 0, 0, null)
    );

    assertException(
        IllegalStateException.class,
        "Null function",
        () -> QuickJS.invokeValueFunction(1, 0, 0, null)
    );

    assertException(
        IllegalStateException.class,
        "Null arguments",
        () -> QuickJS.invokeValueFunction(1, 1, 0, null)
    );
  }

  @Test
  public void testDestroyValue() {
    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.destroyValue(0, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null JSValue",
        () -> QuickJS.destroyValue(1, 0)
    );
  }

  @Test
  public void testGetException() {
    withRuntimeContextScript("throw 1", (runtime, context, value) -> {
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
      JSException jsException = QuickJS.getException(context);
      assertFalse(jsException.isError());
      assertEquals("1", jsException.getException());
      assertNull(jsException.getStack());
    });

    withRuntimeContextScript("throw new Error()", (runtime, context, value) -> {
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));
      JSException jsException = QuickJS.getException(context);
      assertTrue(jsException.isError());
      assertEquals("Error", jsException.getException());
      assertEquals("    at <eval> (source.js)\n", jsException.getStack());
    });

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

    withRuntimeContextScript("1", (runtime, context, value) -> {
      assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value));
      JSException jsException = QuickJS.getException(context);
      assertFalse(jsException.isError());
      assertEquals("null", jsException.getException());
      assertNull(jsException.getStack());
    });

    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.getException(0)
    );
  }

  @Test
  public void testEvaluate() {
    assertException(
        IllegalStateException.class,
        "Null JSContext",
        () -> QuickJS.evaluate(0, null, null, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null source code",
        () -> QuickJS.evaluate(1, null, null, 0)
    );

    assertException(
        IllegalStateException.class,
        "Null file name",
        () -> QuickJS.evaluate(1, "", null, 0)
    );
  }
}
