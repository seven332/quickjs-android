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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class JSContext implements Closeable {

  private static final Class<?>[] PRIMITIVE_CLASSES =
      { boolean.class, int.class, long.class, float.class, double.class };

  private static final Class<?>[] OBJECT_CLASSES =
      { Boolean.class, Integer.class, Long.class, Float.class, Double.class, String.class };

  private long context;
  private NativeCleaner<JSValue> cleaner;

  JSContext(long context) {
    this.context = context;
    this.cleaner = new JSValueCleaner();
  }

  private void checkClosed() {
    if (context == 0) {
      throw new IllegalStateException("The JSContext is closed");
    }
  }

  private boolean valueToBoolean(long value, int tag) {
    if (tag == QuickJS.VALUE_TAG_BOOL) {
      return QuickJS.getValueBoolean(value);
    } else {
      throw new IllegalStateException("Invalid tag for boolean: " + tag);
    }
  }

  private int valueToInt(long value, int tag) {
    if (tag == QuickJS.VALUE_TAG_INT) {
      return QuickJS.getValueInt(value);
    } else {
      throw new IllegalStateException("Invalid tag for int: " + tag);
    }
  }

  private long valueToLong(long value, int tag) {
    if (tag == QuickJS.VALUE_TAG_INT) {
      return QuickJS.getValueInt(value);
    } else if (tag == QuickJS.VALUE_TAG_FLOAT64) {
      // TODO throw exception if it has decimal part
      return (long) QuickJS.getValueDouble(value);
    } else {
      throw new IllegalStateException("Invalid tag for long: " + tag);
    }
  }

  private float valueToFloat(long value, int tag) {
    if (tag == QuickJS.VALUE_TAG_INT) {
      return (float) QuickJS.getValueInt(value);
    } else if (tag == QuickJS.VALUE_TAG_FLOAT64) {
      return (float) QuickJS.getValueDouble(value);
    } else {
      throw new IllegalStateException("Invalid tag for float: " + tag);
    }
  }

  private double valueToDouble(long value, int tag) {
    if (tag == QuickJS.VALUE_TAG_INT) {
      return (double) QuickJS.getValueInt(value);
    } else if (tag == QuickJS.VALUE_TAG_FLOAT64) {
      return QuickJS.getValueDouble(value);
    } else {
      throw new IllegalStateException("Invalid tag for double: " + tag);
    }
  }

  private String valueToString(long value, int tag) {
    if (tag == QuickJS.VALUE_TAG_STRING) {
      return QuickJS.getValueString(context, value);
    } else {
      throw new IllegalStateException("Invalid tag for double: " + tag);
    }
  }

  private static <T> boolean contains(T[] array, T element) {
    for (T t : array) {
      if (t == element) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private <T> T valueToType(long value, JavaType javaType) {
    boolean toDestroyValue = true;

    try {
      int tag = QuickJS.getValueTag(value);

      if (tag == QuickJS.VALUE_TAG_EXCEPTION) {
        throw new QuickJSException(QuickJS.getException(context));
      }

      if (tag == QuickJS.VALUE_TAG_NULL || tag == QuickJS.VALUE_TAG_UNDEFINED) {
        if (javaType.nullable) {
          return null;
        } else {
          throw new IllegalStateException("Null and undefined are not accepted");
        }
      }

      Class<?> type = javaType.type;
      if (type == boolean.class || type == Boolean.class) {
        return (T) (Boolean) valueToBoolean(value, tag);
      } else if (type == int.class || type == Integer.class) {
        return (T) (Integer) valueToInt(value, tag);
      } else if (type == long.class || type == Long.class) {
        return (T) (Long) valueToLong(value, tag);
      } else if (type == float.class || type == Float.class) {
        return (T) (Float) valueToFloat(value, tag);
      } else if (type == double.class || type == Double.class) {
        return (T) (Double) valueToDouble(value, tag);
      } else if (type == String.class) {
        return (T) valueToString(value, tag);
      } else {
        // TODO Interface
        //  Use Proxy to create a instance, add the native pointer to NativeCleaner
        return null;
      }
    } finally {
      if (toDestroyValue) {
        QuickJS.destroyValue(context, value);
      }
    }
  }

  public synchronized <T> T evaluate(String script, String fileName, Class<T> returnType) {
    return evaluate(script, fileName, QuickJS.EVAL_TYPE_GLOBAL, 0, returnType);
  }

  public synchronized <T> T evaluate(String script, String fileName, int type, int flags, Class<T> returnType) {
    checkClosed();

    // Trigger cleaner
    cleaner.clean();

    if (type != QuickJS.EVAL_TYPE_GLOBAL && type != QuickJS.EVAL_TYPE_MODULE) {
      throw new IllegalArgumentException("Invalid type: " + type);
    }
    if ((flags & (~QuickJS.EVAL_FLAG_MASK)) != 0) {
      throw new IllegalArgumentException("Invalid flags: " + flags);
    }

    JavaType javaType = JavaType.from(returnType);

    long value = QuickJS.evaluate(context, script, fileName, type | flags);

    if (value == 0) {
      throw new IllegalStateException("Fail to evaluate the script");
    }

    return valueToType(value, javaType);
  }

  synchronized void destroyValue(long value) {
    checkClosed();
    QuickJS.destroyValue(context, value);
  }

  synchronized int notRemovedJSValueCount() {
    return cleaner.size();
  }

  @Override
  public synchronized void close() {
    if (context != 0) {
      cleaner.forceClean();
      long contextToClose = context;
      context = 0;
      QuickJS.destroyContext(contextToClose);
    }
  }

  private class JSValueCleaner extends NativeCleaner<JSValue> {

    @Override
    public void onRemove(long pointer) {
      destroyValue(pointer);
    }
  }

  static class JavaType {

    final Class<?> type;
    final boolean nullable;
    final Map<String, Method> methods;

    JavaType(Class<?> type, boolean nullable, Map<String, Method> methods) {
      this.type = type;
      this.nullable = nullable;
      this.methods = methods;
    }

    static JavaType from(Class<?> type) {
      boolean nullable;
      Map<String, Method> methods;

      if (type.isInterface()) {
        nullable = true;
        methods = new HashMap<>();
        LinkedList<Class<?>> interfaces = new LinkedList<>();
        interfaces.add(type);

        while (!interfaces.isEmpty()) {
          Class<?> child = interfaces.pollFirst();
          for (Method method : child.getMethods()) {
            Method oldMethod = methods.get(method.getName());
            if (oldMethod != null) {
              // override or overload
              if (!Arrays.equals(method.getParameterTypes(), oldMethod.getParameterTypes())) {
                throw new UnsupportedOperationException(method.getName() + " is overloaded in " + child);
              }

              Class<?> returnType = method.getReturnType();
              Class<?> oldReturnType = oldMethod.getReturnType();
              if (returnType != oldReturnType && oldReturnType.isAssignableFrom(returnType)) {
                // returnType extends oldReturnType
                methods.put(method.getName(), method);
              }
            } else {
              methods.put(method.getName(), method);
            }
          }
        }
      } else if (contains(OBJECT_CLASSES, type)) {
        nullable = true;
        methods = null;
      } else if (contains(PRIMITIVE_CLASSES, type)) {
        nullable = false;
        methods = null;
      } else {
        throw new UnsupportedOperationException("Unsupported type: " + type);
      }

      return new JavaType(type, nullable, methods);
    }
  }
}
