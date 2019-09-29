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

import java.io.Closeable;

/**
 * JSContext is a JavaScript context with its own global objects.
 * JSContexts in the same JSRuntime share the same memory heap.
 *
 * @see JSRuntime
 */
public class JSContext implements Closeable {

  static final int TYPE_SYMBOL = -8;
  static final int TYPE_STRING = -7;
  static final int TYPE_OBJECT = -1;
  static final int TYPE_INT = 0;
  static final int TYPE_BOOLEAN = 1;
  static final int TYPE_NULL = 2;
  static final int TYPE_UNDEFINED = 3;
  static final int TYPE_EXCEPTION = 6;
  static final int TYPE_FLOAT64 = 7;

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

  long pointer;
  final QuickJS quickJS;
  final JSRuntime jsRuntime;
  private final NativeCleaner cleaner;

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

    // Trigger cleaner
    cleaner.clean();

    return pointer;
  }

  public <T> void set(String name, Class<T> javaClass, T object) {
    setInternal(name, quickJS.getTranslator(javaClass), object);
  }

  public <T> void set(String name, GenericType<T> javaType, T object) {
    setInternal(name, quickJS.getTranslator(javaType.type), object);
  }

  private <T> void setInternal(String name, Translator<T> translator, T object) {
    synchronized (jsRuntime) {
      checkClosed();
      BitSink sink = translator.pickle(this, object);
      QuickJS.setContextValue(pointer, name, translator.unpicklePointer, sink.getBytes(), sink.getSize());
    }
  }

  /**
   * Evaluates the script in this JSContext.
   */
  public void evaluate(String script, String fileName) {
    evaluateInternal(script, fileName, EVAL_TYPE_GLOBAL, 0, null);
  }

  /**
   * Evaluates the script in this JSContext.
   *
   * @param type must be one of {@link #EVAL_TYPE_GLOBAL} and {@link #EVAL_TYPE_MODULE}
   * @param flags must be logic and of {@link #EVAL_FLAG_SHEBANG}, {@link #EVAL_FLAG_STRICT} and {@link #EVAL_FLAG_STRIP}
   */
  public void evaluate(String script, String fileName, int type, int flags) {
    evaluateInternal(script, fileName, type, flags, null);
  }

  /**
   * Evaluates the script in this JSContext.
   * Returns the result as the java class.
   */
  public <T> T evaluate(String script, String fileName, Class<T> clazz) {
    return evaluateInternal(script, fileName, EVAL_TYPE_GLOBAL, 0, quickJS.getTranslator(clazz));
  }

  /**
   * Evaluates the script in this JSContext.
   * Returns the result as the java type.
   */
  public <T> T evaluate(String script, String fileName, GenericType<T> javaType) {
    return evaluateInternal(script, fileName, EVAL_TYPE_GLOBAL, 0, quickJS.getTranslator(javaType.type));
  }

  /**
   * Evaluates the script in this JSContext.
   * Returns the result as the java class.
   *
   * @param type must be one of {@link #EVAL_TYPE_GLOBAL} and {@link #EVAL_TYPE_MODULE}
   * @param flags must be logic and of {@link #EVAL_FLAG_SHEBANG}, {@link #EVAL_FLAG_STRICT} and {@link #EVAL_FLAG_STRIP}
   */
  public <T> T evaluate(String script, String fileName, int type, int flags, Class<T> javaClass) {
    return evaluateInternal(script, fileName, type, flags, quickJS.getTranslator(javaClass));
  }

  /**
   * Evaluates the script in this JSContext.
   * Returns the result as the java type.
   *
   * @param type must be one of {@link #EVAL_TYPE_GLOBAL} and {@link #EVAL_TYPE_MODULE}
   * @param flags must be logic and of {@link #EVAL_FLAG_SHEBANG}, {@link #EVAL_FLAG_STRICT} and {@link #EVAL_FLAG_STRIP}
   */
  public <T> T evaluate(String script, String fileName, int type, int flags, GenericType<T> javaType) {
    return evaluateInternal(script, fileName, type, flags, quickJS.getTranslator(javaType.type));
  }

  private <T> T evaluateInternal(String script, String fileName, int type, int flags, @Nullable Translator<T> translator) {
    if (type != EVAL_TYPE_GLOBAL && type != EVAL_TYPE_MODULE) {
      throw new IllegalArgumentException("Invalid type: " + type);
    }
    if ((flags & (~EVAL_FLAG_MASK)) != 0) {
      throw new IllegalArgumentException("Invalid flags: " + flags);
    }

    synchronized (jsRuntime) {
      checkClosed();
      byte[] bytes = QuickJS.evaluate(pointer, script, fileName, type | flags, translator != null ? translator.picklePointer : 0);
      return translator != null ? translator.unpickle(this, bytes) : null;
    }
  }

  void registerJSValue(Object object, long pointer) {
    cleaner.register(object, pointer);
  }

  void unregisterJSValue(long pointer) {
    cleaner.unregister(pointer);
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

  private class JSValueCleaner extends NativeCleaner {

    @Override
    public void onRemove(long pointer) {
      QuickJS.destroyValue(JSContext.this.pointer, pointer);
    }
  }
}
