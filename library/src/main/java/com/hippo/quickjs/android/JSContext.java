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
  private final QuickJS quickJS;
  private final JSRuntime jsRuntime;
  private final NativeCleaner<JSValue> cleaner;

  JSContext(long pointer, QuickJS quickJS, JSRuntime jsRuntime) {
    this.pointer = pointer;
    this.quickJS = quickJS;
    this.jsRuntime = jsRuntime;
    this.cleaner = new JSValueCleaner();
  }

  long checkClosed() {
    if (pointer == 0) {
      throw new IllegalStateException("The JSContext is closed");
    }
    return pointer;
  }

  public <T> T evaluate(String script, String fileName, Class<T> clazz) {
    return evaluate(script, fileName, EVAL_TYPE_GLOBAL, 0, quickJS.<T>getAdapter(clazz));
  }

  public <T> T evaluate(String script, String fileName, TypeAdapter<T> adapter) {
    return evaluate(script, fileName, EVAL_TYPE_GLOBAL, 0, adapter);
  }

  public <T> T evaluate(String script, String fileName, int type, int flags, Class<T> clazz) {
    return evaluate(script, fileName, type, flags, quickJS.<T>getAdapter(clazz));
  }

  /**
   * Evaluates the script in this JSContext.
   * The TypeAdapter converts the result to the target type.
   */
  public <T> T evaluate(String script, String fileName, int type, int flags, TypeAdapter<T> adapter) {
    synchronized (jsRuntime) {
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

      JSValue jsValue = new JSValue(value, jsRuntime, this);
      cleaner.register(jsValue, value);

      return adapter.fromJSValue(jsValue);
    }
  }

  /**
   * Wraps a JSValue c pointer as a Java JSValue object instance.
   */
  JSValue wrapAsJSValue(long value) {
    if (value == 0) {
      throw new IllegalStateException("Can't wrap null pointer as JSValue");
    }

    // Check js exception
    if (QuickJS.getValueTag(value) == JSValue.TYPE_EXCEPTION) {
      throw new JSEvaluationException(QuickJS.getException(pointer));
    }

    JSValue jsValue = new JSValue(value, jsRuntime, this);

    // Register it to cleaner
    cleaner.register(jsValue, value);

    return jsValue;
  }

  int getNotRemovedJSValueCount() {
    synchronized (jsRuntime) {
      return cleaner.size();
    }
  }

  @Override
  public void close() {
    synchronized (jsRuntime) {
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
