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

import java.io.Closeable;

/**
 * JSContext is a JavaScript context with its own global objects.
 * JSContexts in the same JSRuntime share the same memory heap.
 *
 * @see JSRuntime
 */
public class JSContext implements Closeable {

  /**
   * Global code.
   */
  public static final int EVAL_TYPE_GLOBAL = 0 << 0;

  /**
   * Module code.
   */
  public static final int EVAL_TYPE_MODULE = 1 << 0;

  /**
   * Skip first line beginning with '#!'.
   */
  public static final int EVAL_FLAG_SHEBANG = 1 << 2;

  /**
   * Force 'strict' mode.
   */
  public static final int EVAL_FLAG_STRICT = 1 << 3;

  /**
   * Force 'strip' mode.
   *
   * Remove the debug information (including the source code
   * of the functions) to save memory.
   */
  public static final int EVAL_FLAG_STRIP = 1 << 4;

  private static final int EVAL_FLAG_MASK = 0b11100;

  private long pointer;
  private final TypeAdapter.Depot depot;
  private final Object lock;
  private final NativeCleaner<JSValue> cleaner;

  JSContext(long pointer, TypeAdapter.Depot depot, Object lock) {
    this.pointer = pointer;
    this.depot = depot;
    this.lock = lock;
    this.cleaner = new JSValueCleaner();
  }

  private void checkClosed() {
    if (pointer == 0) {
      throw new IllegalStateException("The JSContext is closed");
    }
  }

  public <T> T evaluate(String script, String fileName, Class<T> clazz) {
    return evaluate(script, fileName, EVAL_TYPE_GLOBAL, 0, depot.<T>getAdapter(clazz));
  }

  public <T> T evaluate(String script, String fileName, TypeAdapter<T> adapter) {
    return evaluate(script, fileName, EVAL_TYPE_GLOBAL, 0, adapter);
  }

  public <T> T evaluate(String script, String fileName, int type, int flags, Class<T> clazz) {
    return evaluate(script, fileName, type, flags, depot.<T>getAdapter(clazz));
  }

  /**
   * Evaluates the script in this JSContext.
   * The TypeAdapter converts the result to the target type.
   */
  public <T> T evaluate(String script, String fileName, int type, int flags, TypeAdapter<T> adapter) {
    synchronized (lock) {
      checkClosed();

      // Trigger cleaner
      cleaner.clean();

      if (type != EVAL_TYPE_GLOBAL && type != EVAL_TYPE_MODULE) {
        throw new IllegalArgumentException("Invalid type: " + type);
      }
      if ((flags & (~EVAL_FLAG_MASK)) != 0) {
        throw new IllegalArgumentException("Invalid flags: " + flags);
      }

      long value = QuickJS.evaluate(pointer, script, fileName, type | flags);

      if (value == 0) {
        throw new IllegalStateException("Fail to evaluate the script");
      }

      // Check js exception
      if (QuickJS.getValueTag(value) == JSValue.TYPE_EXCEPTION) {
        throw new JSEvaluationException(QuickJS.getException(pointer));
      }

      JSValue jsValue = new JSValue(value, this);
      cleaner.register(jsValue, value);

      return adapter.fromJSValue(jsValue);
    }
  }

  int getValueTag(long value) {
    synchronized (lock) {
      checkClosed();
      return QuickJS.getValueTag(value);
    }
  }

  boolean isValueArray(long value) {
    synchronized (lock) {
      checkClosed();
      return QuickJS.isValueArray(pointer, value);
    }
  }

  private JSValue processProperty(long value) {
    if (value == 0) {
      throw new IllegalStateException("Fail to get value property");
    }

    // Check js exception
    if (QuickJS.getValueTag(value) == JSValue.TYPE_EXCEPTION) {
      throw new JSEvaluationException(QuickJS.getException(pointer));
    }

    JSValue jsValue = new JSValue(value, this);
    cleaner.register(jsValue, value);

    return jsValue;
  }

  JSValue getValueProperty(long value, int index) {
    synchronized (lock) {
      checkClosed();
      long property = QuickJS.getValueProperty(pointer, value, index);
      return processProperty(property);
    }
  }

  JSValue getValueProperty(long value, String name) {
    synchronized (lock) {
      checkClosed();
      long property = QuickJS.getValueProperty(pointer, value, name);
      return processProperty(property);
    }
  }

  boolean getValueBoolean(long value) {
    synchronized (lock) {
      checkClosed();
      return QuickJS.getValueBoolean(value);
    }
  }

  int getValueInt(long value) {
    synchronized (lock) {
      checkClosed();
      return QuickJS.getValueInt(value);
    }
  }

  double getValueDouble(long value) {
    synchronized (lock) {
      checkClosed();
      return QuickJS.getValueDouble(value);
    }
  }

  String getValueString(long value) {
    synchronized (lock) {
      checkClosed();
      return QuickJS.getValueString(pointer, value);
    }
  }

  int getNotRemovedJSValueCount() {
    synchronized (lock) {
      return cleaner.size();
    }
  }

  @Override
  public void close() {
    synchronized (lock) {
      if (pointer != 0) {
        // Destroy all JSValue
        cleaner.forceClean();
        // Destroy self
        long contextToClose = pointer;
        pointer = 0;
        QuickJS.destroyContext(contextToClose);
      }
    }
  }

  private class JSValueCleaner extends NativeCleaner<JSValue> {

    @Override
    public void onRemove(long pointer) {
      QuickJS.destroyValue(JSContext.this.pointer, pointer);
    }
  }
}
