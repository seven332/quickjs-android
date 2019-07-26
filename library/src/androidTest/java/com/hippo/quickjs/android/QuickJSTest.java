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
  public void testGetValueTag() {
    try {
      QuickJS.getValueTag(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue pointer", e.getMessage());
    }
  }

  private interface JSRunnable {
    void run(long runtime, long context, long value);
  }

  private void runJS(String sourceCode, JSRunnable runnable) {
    long runtime = QuickJS.createRuntime();
    if (runtime != 0) {
      try {
        long context = QuickJS.createContext(runtime);
        if (context != 0) {
          try {
            long value = QuickJS.evaluate(context, sourceCode, "source.js", 0);
            if (value != 0) {
              try {
                runnable.run(runtime, context, value);
              } finally {
                QuickJS.destroyValue(context, value);
              }
            } else {
              fail();
            }
          } finally {
            QuickJS.destroyContext(context);
          }
        } else {
          fail();
        }
      } finally {
        QuickJS.destroyRuntime(runtime);
      }
    } else {
      fail();
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
        } catch (IllegalStateException e) {
          assertEquals("Invalid JSValue tag for boolean: 0", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueBoolean(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue pointer", e.getMessage());
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
        } catch (IllegalStateException e) {
          assertEquals("Invalid JSValue tag for int: 7", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueInt(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue pointer", e.getMessage());
    }
  }

  @Test
  public void testGetValueDouble() {
    runJS("123.1", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        assertEquals(123.1, QuickJS.getValueDouble(value), 0.0001);
      }
    });

    runJS("123", new JSRunnable() {
      @Override
      public void run(long runtime, long context, long value) {
        try {
          QuickJS.getValueDouble(value);
          fail();
        } catch (IllegalStateException e) {
          assertEquals("Invalid JSValue tag for float64: 0", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueDouble(0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSValue pointer", e.getMessage());
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
        } catch (IllegalStateException e) {
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
          assertEquals("Null JSValue pointer", e.getMessage());
        }
      }
    });

    try {
      QuickJS.getValueString(0, 0);
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Null JSContext pointer", e.getMessage());
    }
  }
}
