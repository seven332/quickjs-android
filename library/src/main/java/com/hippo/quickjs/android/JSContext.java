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
public class JSContext implements Closeable, Translator.Context {

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

    // Trigger cleaner
    cleaner.clean();

    return pointer;
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

  private <T> T evaluateInternal(String script, String fileName, int type, int flags, @Nullable Translator<T> adapter) {
    if (type != EVAL_TYPE_GLOBAL && type != EVAL_TYPE_MODULE) {
      throw new IllegalArgumentException("Invalid type: " + type);
    }
    if ((flags & (~EVAL_FLAG_MASK)) != 0) {
      throw new IllegalArgumentException("Invalid flags: " + flags);
    }

    synchronized (jsRuntime) {
      checkClosed();
      byte[] bytes = QuickJS.evaluate(pointer, script, fileName, type | flags, adapter != null ? adapter.picklePointer : 0);
      return adapter != null ? adapter.unpickle(bytes) : null;
    }
  }

  /**
   * Returns the global object.
   */
  public JSObject getGlobalObject() {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.getGlobalObject(pointer);
      return wrapAsJSValue(val).cast(JSObject.class);
    }
  }

  /**
   * Creates a JavaScript undefined.
   */
  @Override
  public JSUndefined createJSUndefined() {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueUndefined(pointer);
      return wrapAsJSValue(val).cast(JSUndefined.class);
    }
  }

  /**
   * Creates a JavaScript null.
   */
  @Override
  public JSNull createJSNull() {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueNull(pointer);
      return wrapAsJSValue(val).cast(JSNull.class);
    }
  }

  /**
   * Creates a JavaScript boolean.
   */
  @Override
  public JSBoolean createJSBoolean(boolean value) {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueBoolean(pointer, value);
      return wrapAsJSValue(val).cast(JSBoolean.class);
    }
  }

  /**
   * Creates a JavaScript number.
   */
  @Override
  public JSNumber createJSNumber(int value) {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueInt(pointer, value);
      return wrapAsJSValue(val).cast(JSNumber.class);
    }
  }

  /**
   * Creates a JavaScript number.
   */
  @Override
  public JSNumber createJSNumber(double value) {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueFloat64(pointer, value);
      return wrapAsJSValue(val).cast(JSNumber.class);
    }
  }

  /**
   * Creates a JavaScript string.
   */
  @Override
  public JSString createJSString(String value) {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueString(pointer, value);
      return wrapAsJSValue(val).cast(JSString.class);
    }
  }

  /**
   * Creates a JavaScript object.
   */
  @Override
  public JSObject createJSObject() {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueObject(pointer);
      return wrapAsJSValue(val).cast(JSObject.class);
    }
  }

  /**
   * Creates a JavaScript object holding a java object.
   */
  @Override
  public JSObject createJSObject(Object object) {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueJavaObject(pointer, object);
      return wrapAsJSValue(val).cast(JSObject.class);
    }
  }

  /**
   * Creates a JavaScript array.
   */
  @Override
  public JSArray createJSArray() {
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueArray(pointer);
      return wrapAsJSValue(val).cast(JSArray.class);
    }
  }

  /**
   * Create a JavaScript function from a java non-static method.
   */
  @Override
  public JSFunction createJSFunction(Object instance, Method method) {
    if (instance == null) throw new NullPointerException("instance == null");
    if (method == null) throw new NullPointerException("method == null");
    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueFunction(pointer, this, instance, method.name, method.getSignature(), method.returnType, method.parameterTypes);
      return wrapAsJSValue(val).cast(JSFunction.class);
    }
  }

  /**
   * Create a JavaScript function from a java static method.
   */
  @Override
  public JSFunction createJSFunctionS(Class clazz, Method method) {
    if (clazz == null) throw new NullPointerException("clazz == null");
    if (method == null) throw new NullPointerException("method == null");

    String name = clazz.getName();
    StringBuilder sb = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      sb.append(c == '.' ? '/' : c);
    }
    String className = sb.toString();

    synchronized (jsRuntime) {
      checkClosed();
      long val = QuickJS.createValueFunctionS(pointer, this, className, method.name, method.getSignature(), method.returnType, method.parameterTypes);
      return wrapAsJSValue(val).cast(JSFunction.class);
    }
  }

  // TODO No need to save c pointers of JSNull, JSUndefined, JSBoolean, JSNumber and JSString.
  //  Just save their types and values.
  /**
   * Wraps a JSValue c pointer as a Java JSValue.
   *
   * @throws JSEvaluationException if it's JS_EXCEPTION
   */
  JSValue wrapAsJSValue(long value) {
    if (value == 0) {
      throw new IllegalStateException("Can't wrap null pointer as JSValue");
    }

    JSValue jsValue;

    int type = QuickJS.getValueTag(value);
    switch (type) {
      case TYPE_SYMBOL:
        jsValue = new JSSymbol(value, this);
        break;
      case TYPE_STRING:
        jsValue = new JSString(value, this, QuickJS.getValueString(pointer, value));
        break;
      case TYPE_OBJECT:
        if (QuickJS.isValueFunction(pointer, value)) {
          jsValue = new JSFunction(value, this);
        } else if (QuickJS.isValueArray(pointer, value)) {
          jsValue = new JSArray(value, this);
        } else {
          jsValue = new JSObject(value, this, QuickJS.getValueJavaObject(pointer, value));
        }
        break;
      case TYPE_INT:
        jsValue = new JSInt(value, this, QuickJS.getValueInt(value));
        break;
      case TYPE_BOOLEAN:
        jsValue = new JSBoolean(value, this, QuickJS.getValueBoolean(value));
        break;
      case TYPE_NULL:
        jsValue = new JSNull(value, this);
        break;
      case TYPE_UNDEFINED:
        jsValue = new JSUndefined(value, this);
        break;
      case TYPE_EXCEPTION:
        QuickJS.destroyValue(pointer, value);
        throw new JSEvaluationException(QuickJS.getException(pointer));
      case TYPE_FLOAT64:
        jsValue = new JSFloat64(value, this, QuickJS.getValueFloat64(value));
        break;
      default:
        jsValue = new JSInternal(value, this);
        break;
    }

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
