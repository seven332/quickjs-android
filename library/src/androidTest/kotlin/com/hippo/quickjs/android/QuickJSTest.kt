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

package com.hippo.quickjs.android

import org.junit.Assert.*
import org.junit.Test

class QuickJSTest {

  @Test
  fun testCreateRuntime() {
    val runtime = QuickJS.createRuntime()
    assertNotEquals(0, runtime)
    QuickJS.destroyRuntime(runtime)
  }

  @Test
  fun testDestroyRuntime() {
    assertException(IllegalStateException::class.java, "Null JSRuntime") {
      QuickJS.destroyRuntime(0)
    }
  }

  private inline fun withRuntime(block: (runtime: Long) -> Unit) {
    val runtime: Long = QuickJS.createRuntime()
    assertNotEquals(0, runtime)
    try {
      block(runtime)
    } finally {
      QuickJS.destroyRuntime(runtime)
    }
  }

  @Test
  fun testCreateContext() {
    withRuntime { runtime ->
      val context = QuickJS.createContext(runtime)
      assertNotEquals(0, context)
      QuickJS.destroyContext(context)
    }
  }

  @Test
  fun testDestroyContext() {
    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.destroyContext(0)
    }
  }

  private inline fun withRuntimeContext(block: (runtime: Long, context: Long) -> Unit) {
    withRuntime { runtime ->
      val context: Long = QuickJS.createContext(runtime)
      assertNotEquals(0, context)
      try {
        block(runtime, context)
      } finally {
        QuickJS.destroyContext(context)
      }
    }
  }

  private inline fun withValue(context: Long, value: Long, block: (value: Long) -> Unit) {
    assertNotEquals(0, value)
    try {
      block(value)
    } finally {
      QuickJS.destroyValue(context, value)
    }
  }

  @Test
  fun testCreateValueUndefined() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueUndefined(context)) { value ->
        assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueUndefined(0)
    }
  }

  @Test
  fun testCreateValueNull() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueNull(context)) { value ->
        assertEquals(JSContext.TYPE_NULL, QuickJS.getValueTag(value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueNull(0)
    }
  }

  @Test
  fun testCreateValueBoolean() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueBoolean(context, true)) { value ->
        assertTrue(QuickJS.getValueBoolean(value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueBoolean(0, true)
    }
  }

  @Test
  fun testCreateValueInt() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueInt(context, 32)) { value ->
        assertEquals(32, QuickJS.getValueInt(value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueInt(0, 32)
    }
  }

  @Test
  fun testCreateValueFloat64() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueFloat64(context, 1.11)) { value ->
        assertEquals(1.11, QuickJS.getValueFloat64(value), 0.0)
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueFloat64(0, 1.11)
    }
  }

  @Test
  fun testCreateValueString() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueString(context, "string")) { value ->
        assertEquals("string", QuickJS.getValueString(context, value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueString(0, null)
    }

    assertException(IllegalStateException::class.java, "Null value") {
      QuickJS.createValueString(1, null)
    }
  }

  @Test
  fun testCreateValueObject() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueObject(context)) { value ->
        assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueObject(0)
    }
  }

  @Test
  fun testCreateValueArray() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueArray(context)) { value ->
        assertTrue(QuickJS.isValueArray(context, value))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.createValueArray(0)
    }
  }

  @Test
  fun testGetValueTag() {
    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueTag(0)
    }
  }

  private inline fun withScript(context: Long, script: String, block: (value: Long) -> Unit) {
    val value: Long = QuickJS.evaluate(context, script, "source.js", 0)
    assertNotEquals(0, value)
    try {
      block(value)
    } finally {
      QuickJS.destroyValue(context, value)
    }
  }

  private inline fun withRuntimeContextScript(script: String, block: (runtime: Long, context: Long, value: Long) -> Unit) {
    withRuntimeContext { runtime, context ->
      withScript(context, script) { value ->
        block(runtime, context, value)
      }
    }
  }

  @Test
  fun testIsValueArray() {
    withRuntimeContextScript("[]") { _, context, value ->
      assertTrue(QuickJS.isValueArray(context, value))
    }

    withRuntimeContextScript("false") { _, context, value ->
      assertFalse(QuickJS.isValueArray(context, value))
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.isValueArray(0, 0)
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.isValueArray(1, 0)
    }
  }

  @Test
  fun testIsValueFunction() {
    withRuntimeContextScript("b = function(){}") { _, context, value ->
      assertTrue(QuickJS.isValueFunction(context, value))
    }

    withRuntimeContextScript("false") { _, context, value ->
      assertFalse(QuickJS.isValueFunction(context, value))
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.isValueFunction(0, 0)
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.isValueFunction(1, 0)
    }
  }


  private fun withProperty(context: Long, value: Long, index: Int, block: (property: Long) -> Unit) {
    val property = QuickJS.getValueProperty(context, value, index)
    assertNotEquals(0, property)
    try {
      block(property)
    } finally {
      QuickJS.destroyValue(context, property)
    }
  }

  private fun withProperty(context: Long, value: Long, name: String, block: (property: Long) -> Unit) {
    val property = QuickJS.getValueProperty(context, value, name)
    assertNotEquals(0, property)
    try {
      block(property)
    } finally {
      QuickJS.destroyValue(context, property)
    }
  }

  @Test
  fun testGetValuePropertyInt() {
    withRuntimeContextScript("[1, 'str']") { _, context, value ->
      withProperty(context, value, 0) { property ->
        assertEquals(1, QuickJS.getValueInt(property))
      }
      withProperty(context, value, 1) { property ->
        assertEquals("str", QuickJS.getValueString(context, property))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.getValueProperty(0, 0, 0)
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueProperty(1, 0, 0)
    }
  }

  @Test
  fun testGetValuePropertyString() {
    withRuntimeContextScript("a = {a: 1, b: 'str'}") { _, context, value ->
      withProperty(context, value, "a") { property ->
        assertEquals(1, QuickJS.getValueInt(property))
      }
      withProperty(context, value, "b") { property ->
        assertEquals("str", QuickJS.getValueString(context, property))
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.getValueProperty(0, 0, null)
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueProperty(1, 0, null)
    }

    assertException(IllegalStateException::class.java, "Null name") {
      QuickJS.getValueProperty(1, 1, null)
    }
  }

  @Test
  fun testSetValuePropertyInt() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueObject(context)) { value ->
        withValue(context, QuickJS.createValueObject(context)) { property ->
          withProperty(context, value, 1) { p ->
            assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(p))
          }

          assertTrue(QuickJS.setValueProperty(context, value, 1, property))

          withProperty(context, value, 1) { p ->
            assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(p))
          }
        }
      }
    }

    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueUndefined(context)) { value ->
        withValue(context, QuickJS.createValueObject(context)) { property ->
          assertFalse(QuickJS.setValueProperty(context, value, 1, property))
          assertEquals("TypeError: value has no property\n", QuickJS.getException(context).toString())
        }
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null JSContext") {
        QuickJS.setValueProperty(0, 0, 0, 0)
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null JSValue") {
        QuickJS.setValueProperty(1, 0, 0, 0)
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null property") {
        QuickJS.setValueProperty(1, 1, 0, 0)
      }
    }
  }


  @Test
  fun testSetValuePropertyString() {
    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueObject(context)) { value ->
        withValue(context, QuickJS.createValueObject(context)) { property ->
          withProperty(context, value, "prop") { p ->
            assertEquals(JSContext.TYPE_UNDEFINED, QuickJS.getValueTag(p))
          }

          assertTrue(QuickJS.setValueProperty(context, value, "prop", property))

          withProperty(context, value, "prop") { p ->
            assertEquals(JSContext.TYPE_OBJECT, QuickJS.getValueTag(p))
          }
        }
      }
    }

    withRuntimeContext { _, context ->
      withValue(context, QuickJS.createValueUndefined(context)) { value ->
        withValue(context, QuickJS.createValueObject(context)) { property ->
          assertFalse(QuickJS.setValueProperty(context, value, "prop", property))
          assertEquals("TypeError: value has no property\n", QuickJS.getException(context).toString())
        }
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null JSContext") {
        QuickJS.setValueProperty(0, 0, null, 0)
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null JSValue") {
        QuickJS.setValueProperty(1, 0, null, 0)
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null name") {
        QuickJS.setValueProperty(1, 1, null, 0)
      }
    }

    withRuntimeContext { _, _ ->
      assertException(IllegalStateException::class.java, "Null property") {
        QuickJS.setValueProperty(1, 1, "prop", 0)
      }
    }
  }

  @Test
  fun testGetValueBoolean() {
    withRuntimeContextScript("true") { _, _, value ->
      assertTrue(QuickJS.getValueBoolean(value))
    }

    withRuntimeContextScript("false") { _, _, value ->
      assertFalse(QuickJS.getValueBoolean(value))
    }

    withRuntimeContextScript("1") { _, _, value ->
      assertException(JSDataException::class.java, "Invalid JSValue tag for boolean: 0") {
        QuickJS.getValueBoolean(value)
      }
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueBoolean(0)
    }
  }

  @Test
  fun testGetValueInt() {
    withRuntimeContextScript("123") { _, _, value ->
      assertEquals(123, QuickJS.getValueInt(value))
    }

    withRuntimeContextScript("123.1") { _, _, value ->
      assertException(JSDataException::class.java, "Invalid JSValue tag for int: 7") {
        QuickJS.getValueInt(value)
      }
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueInt(0)
    }
  }

  @Test
  fun testGetValueFloat64() {
    withRuntimeContextScript("123.1") { _, _, value ->
      assertEquals(123.1, QuickJS.getValueFloat64(value), 0.0)
    }

    withRuntimeContextScript("123") { _, _, value ->
      assertException(JSDataException::class.java, "Invalid JSValue tag for float64: 0") {
        QuickJS.getValueFloat64(value)
      }
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueFloat64(0)
    }
  }

  @Test
  fun testGetValueString() {
    withRuntimeContextScript("'string'") { _, context, value ->
      assertEquals("string", QuickJS.getValueString(context, value))
    }

    withRuntimeContextScript("123") { _, context, value ->
      assertException(JSDataException::class.java, "Invalid JSValue tag for string: 0") {
        QuickJS.getValueString(context, value)
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.getValueString(0, 0)
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.getValueString(1, 0)
    }
  }

  @Test
  fun testCallValueFunction() {
    withRuntimeContextScript("f=function a(i,j){return i*j}") { _, context, function ->
      withScript(context, "3") { valueI ->
        withScript(context, "9") { valueJ ->
          withValue(context, QuickJS.invokeValueFunction(context, function, 0, longArrayOf(valueI, valueJ))) { ret ->
            assertEquals(27, QuickJS.getValueInt(ret))
          }
        }
      }
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.invokeValueFunction(0, 0, 0, null)
    }

    assertException(IllegalStateException::class.java, "Null function") {
      QuickJS.invokeValueFunction(1, 0, 0, null)
    }

    assertException(IllegalStateException::class.java, "Null arguments") {
      QuickJS.invokeValueFunction(1, 1, 0, null)
    }
  }

  @Test
  fun testDestroyValue() {
    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.destroyValue(0, 0)
    }

    assertException(IllegalStateException::class.java, "Null JSValue") {
      QuickJS.destroyValue(1, 0)
    }
  }

  @Test
  fun testGetException() {
    withRuntimeContextScript("throw 1") { _, context, value ->
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value))
      val jsException = QuickJS.getException(context)
      assertFalse(jsException.isError)
      assertEquals("1", jsException.exception)
      assertNull(jsException.stack)
    }

    withRuntimeContextScript("throw new Error()") { _, context, value ->
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value))
      val jsException = QuickJS.getException(context)
      assertTrue(jsException.isError)
      assertEquals("Error", jsException.exception)
      assertEquals("    at <eval> (source.js)\n", jsException.stack)
    }

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
        "})()"
    ) { _, context, value ->
      assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value))
      val jsException = QuickJS.getException(context)
      assertTrue(jsException.isError)
      assertEquals("Error", jsException.exception)
      assertEquals(("" +
          "    at f1 (source.js:3)\n" +
          "    at f2 (source.js:6)\n" +
          "    at f3 (source.js:9)\n" +
          "    at <anonymous> (source.js:11)\n" +
          "    at <eval> (source.js:12)\n"), jsException.stack
      )
    }

    withRuntimeContextScript("1") { _, context, value ->
      assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value))
      val jsException = QuickJS.getException(context)
      assertFalse(jsException.isError)
      assertEquals("null", jsException.exception)
      assertNull(jsException.stack)
    }

    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.getException(0)
    }
  }

  @Test
  fun testEvaluate() {
    assertException(IllegalStateException::class.java, "Null JSContext") {
      QuickJS.evaluate(0, null, null, 0)
    }

    assertException(IllegalStateException::class.java, "Null source code") {
      QuickJS.evaluate(1, null, null, 0)
    }

    assertException(IllegalStateException::class.java, "Null file name") {
      QuickJS.evaluate(1, "", null, 0)
    }
  }
}
