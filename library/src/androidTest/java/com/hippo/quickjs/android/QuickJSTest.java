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

import static org.junit.Assert.*;

public class QuickJSTest {

  @Test
  public void testCreateRuntime() {
    long runtime = 0;
    try {
      runtime = QuickJS.createRuntime();
    } finally {
      assertNotEquals(0, runtime);
      QuickJS.destroyRuntime(runtime);
    }
  }

  @Test
  public void testDestroyRuntime() {
    try {
      QuickJS.destroyRuntime(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSRuntime", e.getMessage());
    }
  }

  @Test
  public void testCreateContext() {
    long runtime = 0;
    try {
      runtime = QuickJS.createRuntime();
      long context = 0;
      try {
        context = QuickJS.createContext(runtime);
      } finally {
        assertNotEquals(0, context);
        QuickJS.destroyContext(context);
      }
    } finally {
      assertNotEquals(0, runtime);
      QuickJS.destroyRuntime(runtime);
    }
  }

  @Test
  public void testDestroyContext() {
    try {
      QuickJS.destroyContext(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }
  }

  @Test
  public void testGetValueTag() {
    try {
      QuickJS.getValueTag(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  private interface JSRunnable {
    void run(long runtime, long context, long value);
  }

  private void runJS(String sourceCode, JSRunnable runnable) {
    long runtime = QuickJS.createRuntime();
    assertNotEquals(0, runtime);
    try {
      long context = QuickJS.createContext(runtime);
      assertNotEquals(0, context);
      try {
        long value = QuickJS.evaluate(context, sourceCode, "source.js", 0);
        assertNotEquals(0, value);
        try {
          runnable.run(runtime, context, value);
        } finally {
          QuickJS.destroyValue(context, value);
        }
      } finally {
        QuickJS.destroyContext(context);
      }
    } finally {
      QuickJS.destroyRuntime(runtime);
    }
  }

  @Test
  public void testIsValueArray() {
    runJS("[]", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertTrue(QuickJS.isValueArray(context, value));
      }
    });
    runJS("false", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertFalse(QuickJS.isValueArray(context, value));
      }
    });

    try {
      QuickJS.isValueArray(0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.isValueArray(1, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testIsValueFunction() {
    runJS("b = function(){}", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertTrue(QuickJS.isValueFunction(context, value));
      }
    });
    runJS("false", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertFalse(QuickJS.isValueFunction(context, value));
      }
    });

    try {
      QuickJS.isValueFunction(0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.isValueFunction(1, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  private interface ValueRunnable {
    void run(long value);
  }

  private void withEvaluation(long context, String sourceCode, ValueRunnable runnable) {
    long value = QuickJS.evaluate(context, sourceCode, "test.js", 0);
    assertNotEquals(0, value);
    try {
      runnable.run(value);
    } finally {
      QuickJS.destroyValue(context, value);
    }
  }

  private void withProperty(long context, long value, int index, ValueRunnable runnable) {
    long property = QuickJS.getValueProperty(context, value, index);
    assertNotEquals(0, property);
    try {
      runnable.run(property);
    } finally {
      QuickJS.destroyValue(context, property);
    }
  }

  private void withProperty(long context, long value, String name, ValueRunnable runnable) {
    long property = QuickJS.getValueProperty(context, value, name);
    assertNotEquals(0, property);
    try {
      runnable.run(property);
    } finally {
      QuickJS.destroyValue(context, property);
    }
  }

  @Test
  public void testGetValuePropertyInt() {
    runJS("[1, 'str']", new JSRunnable() {
      @Override
      public void run(long runtime, final long context, long value) {
        withProperty(context, value, 0, new ValueRunnable() {
          @Override
          public void run(long property) {
            assertEquals(1, QuickJS.getValueInt(property));
          }
        });
        withProperty(context, value, 1, new ValueRunnable() {
          @Override
          public void run(long property) {
            assertEquals("str", QuickJS.getValueString(context, property));
          }
        });
      }
    });

    try {
      QuickJS.getValueProperty(0, 0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.getValueProperty(1, 0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testGetValuePropertyString() {
    runJS("a = {a: 1, b: 'str'}", new JSRunnable() {
      @Override
      public void run(long runtime, final long context, long value) {
        withProperty(context, value, "a", new ValueRunnable() {
          @Override
          public void run(long property) {
            assertEquals(1, QuickJS.getValueInt(property));
          }
        });
        withProperty(context, value, "b", new ValueRunnable() {
          @Override
          public void run(long property) {
            assertEquals("str", QuickJS.getValueString(context, property));
          }
        });
      }
    });

    try {
      QuickJS.getValueProperty(0, 0, null);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.getValueProperty(1, 0, null);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }

    try {
      QuickJS.getValueProperty(1, 1, null);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null name", e.getMessage());
    }
  }

  @Test
  public void testGetValueBoolean() {
    runJS("true", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertTrue(QuickJS.getValueBoolean(value));
      }
    });

    runJS("false", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertFalse(QuickJS.getValueBoolean(value));
      }
    });

    runJS("1", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        try {
          QuickJS.getValueBoolean(value);
          fail();
        } catch (JSDataException e) {
          assertEquals("Invalid JSValue tag for boolean: 0", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueBoolean(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testGetValueInt() {
    runJS("123", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(123, QuickJS.getValueInt(value));
      }
    });

    runJS("123.1", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        try {
          QuickJS.getValueInt(value);
          fail();
        } catch (JSDataException e) {
          assertEquals("Invalid JSValue tag for int: 7", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueInt(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testGetValueFloat64() {
    runJS("123.1", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(123.1, QuickJS.getValueFloat64(value), 0.0001);
      }
    });

    runJS("123", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        try {
          QuickJS.getValueFloat64(value);
          fail();
        } catch (JSDataException e) {
          assertEquals("Invalid JSValue tag for double: 0", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueFloat64(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testGetValueString() {
    runJS("'string'", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals("string", QuickJS.getValueString(context, value));
      }
    });

    runJS("123", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        try {
          QuickJS.getValueString(context, value);
          fail();
        } catch (JSDataException e) {
          assertEquals("Invalid JSValue tag for string: 0", e.getMessage());
        }
      }
    });

    runJS("123", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        try {
          QuickJS.getValueString(context, 0);
          fail();
        } catch (IllegalStateException e) {
          assertEquals("Null JSValue", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueString(0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.getValueString(1, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testCallValueFunction() {
    runJS("f=function a(i,j){return i*j}", new JSRunnable() {
      @Override
      public void run(long runtime, final long context, final long function) {
        withEvaluation(context, "3", new ValueRunnable() {
          @Override
          public void run(final long valueI) {
            withEvaluation(context, "9", new ValueRunnable() {
              @Override
              public void run(long valueJ) {
                long ret = QuickJS.invokeValueFunction(context, function, 0, new long[] { valueI, valueJ });
                assertNotEquals(0, ret);
                try {
                  assertEquals(27, QuickJS.getValueInt(ret));
                } finally {
                  QuickJS.destroyValue(context, ret);
                }
              }
            });
          }
        });
      }
    });

    try {
      QuickJS.invokeValueFunction(0, 0, 0, null);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.invokeValueFunction(1, 0, 0, null);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null function", e.getMessage());
    }

    try {
      QuickJS.invokeValueFunction(1, 1, 0, null);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null arguments", e.getMessage());
    }
  }

  @Test
  public void testDestroyValue() {
    try {
      QuickJS.destroyValue(0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.destroyValue(1, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue", e.getMessage());
    }
  }

  @Test
  public void testGetException() {
    runJS("throw 1", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));

        JSException jsException = QuickJS.getException(context);
        assertFalse(jsException.isError());
        assertEquals("1", jsException.getException());
        assertNull(jsException.getStack());
      }
    });

    runJS("throw new Error()", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));

        JSException jsException = QuickJS.getException(context);
        assertTrue(jsException.isError());
        assertEquals("Error", jsException.getException());
        assertEquals("    at <eval> (source.js)\n",jsException.getStack());
      }
    });

    runJS("throw new Error()", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));

        JSException jsException = QuickJS.getException(context);
        assertTrue(jsException.isError());
        assertEquals("Error", jsException.getException());
        assertEquals("    at <eval> (source.js)\n",jsException.getStack());
      }
    });

    runJS("(function() {\n" +
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
        "})()", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(JSContext.TYPE_EXCEPTION, QuickJS.getValueTag(value));

        JSException jsException = QuickJS.getException(context);
        assertTrue(jsException.isError());
        assertEquals("Error", jsException.getException());
        assertEquals("    at f1 (source.js:3)\n" +
            "    at f2 (source.js:6)\n" +
            "    at f3 (source.js:9)\n" +
            "    at <anonymous> (source.js:11)\n" +
            "    at <eval> (source.js:12)\n",jsException.getStack());
      }
    });

    runJS("1", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(JSContext.TYPE_INT, QuickJS.getValueTag(value));

        JSException jsException = QuickJS.getException(context);
        assertFalse(jsException.isError());
        assertEquals("null", jsException.getException());
        assertNull(jsException.getStack());
      }
    });

    try {
      QuickJS.getException(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }
  }

  @Test
  public void testEvaluate() {
    try {
      QuickJS.evaluate(0, null, null, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext", e.getMessage());
    }

    try {
      QuickJS.evaluate(1, null, null, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null source code", e.getMessage());
    }

    try {
      QuickJS.evaluate(1, "", null, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null file name", e.getMessage());
    }
  }
}
