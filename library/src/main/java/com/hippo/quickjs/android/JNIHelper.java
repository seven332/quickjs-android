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

import java.lang.reflect.Type;

class JNIHelper {

  private static Type VOID_PRIMITIVE_TYPE = void.class;
  private static Type CHAR_PRIMITIVE_TYPE = char.class;
  private static Type BOOLEAN_PRIMITIVE_TYPE = boolean.class;
  private static Type BYTE_PRIMITIVE_TYPE = byte.class;
  private static Type SHORT_PRIMITIVE_TYPE = short.class;
  private static Type INT_PRIMITIVE_TYPE = int.class;
  private static Type LONG_PRIMITIVE_TYPE = long.class;
  private static Type FLOAT_PRIMITIVE_TYPE = float.class;
  private static Type DOUBLE_PRIMITIVE_TYPE = double.class;

  private static Object jsValueToJavaValue(JSContext jsContext, Type type, long value) {
    synchronized (jsContext.jsRuntime) {
      JSValue jsValue = null;
      try {
        jsContext.checkClosed();
        TypeAdapter<Object> adapter = jsContext.quickJS.getAdapter(type);
        jsValue = jsContext.wrapAsJSValue(value);
        return adapter.fromJSValue(jsContext, jsValue);
      } finally {
        if (jsValue == null) {
          QuickJS.destroyValue(jsContext.pointer, value);
        }
      }
    }
  }

  private static long javaValueToJSValue(JSContext jsContext, Type type, boolean value) { return javaValueToJSValue(jsContext, type, (Boolean) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, char value) { return javaValueToJSValue(jsContext, type, (Character) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, byte value) { return javaValueToJSValue(jsContext, type, (Byte) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, short value) { return javaValueToJSValue(jsContext, type, (Short) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, int value) { return javaValueToJSValue(jsContext, type, (Integer) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, long value) { return javaValueToJSValue(jsContext, type, (Long) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, float value) { return javaValueToJSValue(jsContext, type, (Float) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, double value) { return javaValueToJSValue(jsContext, type, (Double) value); }
  private static long javaValueToJSValue(JSContext jsContext, Type type, Object value) {
    synchronized (jsContext.jsRuntime) {
      jsContext.checkClosed();
      TypeAdapter<Object> adapter = jsContext.quickJS.getAdapter(type);
      return adapter.toJSValue(jsContext, value).pointer;
    }
  }

  private static boolean isPrimitiveType(Type type) {
    return type instanceof Class && ((Class<?>) type).isPrimitive();
  }

  @SuppressWarnings("EqualsReplaceableByObjectsCall")
  private static boolean isSameType(Type t1, Type t2) {
    return (t1 == t2) || (t1 != null && t1.equals(t2));
  }

  private static boolean unbox(Boolean value) { return value; }
  private static char unbox(Character value) { return value; }
  private static byte unbox(Byte value) { return value; }
  private static short unbox(Short value) { return value; }
  private static int unbox(Integer value) { return value; }
  private static long unbox(Long value) { return value; }
  private static float unbox(Float value) { return value; }
  private static double unbox(Double value) { return value; }
}
