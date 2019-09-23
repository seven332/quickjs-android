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

import java.lang.reflect.Type;

/**
 * Translator pickle a JSValue/JavaValue to a byte array in C/Java,
 * then unpickle the byte array to a JavaValue/JSValue in Java/C.
 */
public abstract class Translator<T> {

  static final Placeholder[] EMPTY_PLACEHOLDER_ARRAY = new Placeholder[0];
  static final int MAX_INLINE_SIZE = 8 * 1024; // TODO A better number

  public static final byte PICKLE_FLAG_PROP_INT          = (byte) 0b00000000;
  public static final byte PICKLE_FLAG_PROP_STR          = (byte) 0b00000001;
  public static final byte PICKLE_FLAG_TYPE_NULL         = (byte) 0b10000000;
  public static final byte PICKLE_FLAG_TYPE_BOOLEAN      = (byte) 0b10000001;
  public static final byte PICKLE_FLAG_TYPE_NUMBER       = (byte) 0b10000010;
  public static final byte PICKLE_FLAG_TYPE_STRING       = (byte) 0b10000011;
  public static final byte PICKLE_FLAG_TYPE_OBJECT       = (byte) 0b10000100;
  public static final byte PICKLE_FLAG_TYPE_ARRAY        = (byte) 0b10000101;
  public static final byte PICKLE_FLAG_TYPE_COMMAND      = (byte) 0b10000110;
  public static final byte PICKLE_FLAG_ATTR_NULLABLE     = (byte) 0b01000000;
  public static final byte PICKLE_FLAG_OPT_PUSH          = (byte) 0b11000000;
  public static final byte PICKLE_FLAG_OPT_POP           = (byte) 0b11000001;

  public static final byte UNPICKLE_FLAG_PROP_INT         = (byte) 0b00000000;
  public static final byte UNPICKLE_FLAG_PROP_STR         = (byte) 0b00000001;
  public static final byte UNPICKLE_FLAG_TYPE_NULL        = (byte) 0b10000000;
  public static final byte UNPICKLE_FLAG_TYPE_BOOLEAN     = (byte) 0b10000001;
  public static final byte UNPICKLE_FLAG_TYPE_BYTE        = (byte) 0b10000010;
  public static final byte UNPICKLE_FLAG_TYPE_SHORT       = (byte) 0b10000011;
  public static final byte UNPICKLE_FLAG_TYPE_INT         = (byte) 0b10000100;
  public static final byte UNPICKLE_FLAG_TYPE_FLOAT       = (byte) 0b10000101;
  public static final byte UNPICKLE_FLAG_TYPE_DOUBLE      = (byte) 0b10000110;
  public static final byte UNPICKLE_FLAG_TYPE_STRING      = (byte) 0b10000111;
  public static final byte UNPICKLE_FLAG_TYPE_OBJECT      = (byte) 0b10001000;
  public static final byte UNPICKLE_FLAG_TYPE_ARRAY       = (byte) 0b10001001;
  public static final byte UNPICKLE_FLAG_TYPE_COMMAND     = (byte) 0b10001010;
  public static final byte UNPICKLE_FLAG_ATTR_NULLABLE    = (byte) 0b01000000;
  public static final byte UNPICKLE_FLAG_OPT_PUSH         = (byte) 0b11000000;
  public static final byte UNPICKLE_FLAG_OPT_POP          = (byte) 0b11000001;

  final byte[] pickleCommand;
  final byte[] unpickleCommand;
  final Placeholder[] placeholders;

  long picklePointer;
  long unpicklePointer;

  Translator(byte[] pickleCommand, byte[] unpickleCommand) {
    this(pickleCommand, unpickleCommand, EMPTY_PLACEHOLDER_ARRAY);
  }

  Translator(byte[] pickleCommand, byte[] unpickleCommand, Placeholder[] placeholders) {
    this.pickleCommand = pickleCommand;
    this.unpickleCommand = unpickleCommand;
    this.placeholders = placeholders;
  }

  protected abstract T unpickle(JSContext context, BitSource source);

  final T unpickle(JSContext context, byte[] bytes) {
    BitSource source = new BitSource(bytes);
    T value = unpickle(context, source);
    source.checkEOF();
    return value;
  }

  protected abstract void pickle(JSContext context, T value, BitSink sink);

  final BitSink pickle(JSContext context, T value) {
    BitSink sink = new BitSink();
    pickle(context, value, sink);
    return sink;
  }

  public static class Placeholder {
    public final Type type;
    public final int pickleIndex;
    public final int unpickleIndex;

    public Placeholder(Type type, int pickleIndex, int unpickleIndex) {
      this.type = type;
      this.pickleIndex = pickleIndex;
      this.unpickleIndex = unpickleIndex;
    }
  }

  public interface Factory {
    @Nullable
    Translator<?> create(Depot depot, Type type);
  }

  public interface Depot {
    <T> Translator<T> getTranslator(Type type);
  }

  public interface Context {

    /**
     * Creates a JavaScript undefined.
     */
    JSUndefined createJSUndefined();

    /**
     * Creates a JavaScript null.
     */
    JSNull createJSNull();

    /**
     * Creates a JavaScript boolean.
     */
    JSBoolean createJSBoolean(boolean value);

    /**
     * Creates a JavaScript number.
     */
    JSNumber createJSNumber(int value);

    /**
     * Creates a JavaScript number.
     */
    JSNumber createJSNumber(double value);

    /**
     * Creates a JavaScript string.
     */
    JSString createJSString(String value);

    /**
     * Creates a JavaScript object.
     */
    JSObject createJSObject();

    /**
     * Creates a JavaScript object holding a java object.
     */
    JSObject createJSObject(Object object);

    /**
     * Creates a JavaScript array.
     */
    JSArray createJSArray();

    /**
     * Create a JavaScript function from a java non-static method.
     */
    JSFunction createJSFunction(Object instance, Method method);

    /**
     * Create a JavaScript function from a java static method.
     */
    JSFunction createJSFunctionS(Class clazz, Method method);
  }
}
