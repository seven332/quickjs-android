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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

class InterfaceTranslator extends Translator<Object> {

  private static final byte[] PICKLE_COMMAND = new byte[] { Translator.PICKLE_FLAG_TYPE_OBJECT };
  private static final byte[] UNPICKLE_COMMAND = new byte[] { Translator.UNPICKLE_FLAG_TYPE_OBJECT };

  private static final Object[] EMPTY_ARGS = new Object[0];

  private static Type nonNullOfIfNotPrimitive(Type type) {
    if (type instanceof Class && ((Class) type).isPrimitive()) {
      return type;
    } else {
      return Types.nonNullOf(type);
    }
  }

  private static boolean typeArrayEquals(Type[] a, Type[] b) {
    int length = a.length;
    if (b.length != length) return false;

    for (int i = 0; i < length; i++) {
      if (!Types.equals(a[i], b[i])) return false;
    }

    return true;
  }

  /**
   * Returns all methods in the interface type.
   * Returns {@code null} if the type is not interface,
   * or any method is overloaded, or any type can't be resolved.
   */
  @Nullable
  static Map<String, Method> getInterfaceMethods(Translator.Depot depot, Type type) {
    Class<?> rawType = Types.getRawType(type);
    if (!rawType.isInterface()) return null;

    Map<String, Method> methods = new HashMap<>();

    for (java.lang.reflect.Method method : rawType.getMethods()) {
      // Return type and return type translator
      Type returnType = Types.resolve(type, rawType, method.getGenericReturnType());
      if (returnType instanceof TypeVariable) {
        throw new IllegalArgumentException("Can't resolve return type");
      }
      returnType = nonNullOfIfNotPrimitive(returnType);
      Translator<Object> returnTranslator = depot.getTranslator(returnType);

      // Method name
      String name = method.getName();

      Type[] originParameterTypes = method.getGenericParameterTypes();
      int parameterTypeSize = originParameterTypes.length;

      if (name.equals("close") && (parameterTypeSize != 0 || !Closeable.class.isAssignableFrom(rawType))) {
        throw new IllegalArgumentException("Only close() method declared from Closeable is accepted");
      }

      Type[] parameterTypes = new Type[parameterTypeSize];
      for (int i = 0; i < parameterTypes.length; i++) {
        parameterTypes[i] = Types.resolve(type, rawType, originParameterTypes[i]);
        if (parameterTypes[i] instanceof TypeVariable) {
          throw new IllegalArgumentException("Can't resolve parameter type");
        }
        parameterTypes[i] = nonNullOfIfNotPrimitive(parameterTypes[i]);
      }

      Method oldMethod = methods.get(name);
      if (oldMethod != null) {
        if (!typeArrayEquals(oldMethod.parameterTypes, parameterTypes)) {
          throw new IllegalArgumentException("Overload is not supported");
        }
        if (Types.equals(returnType, oldMethod.returnType)
            || Types.getRawType(returnType).isAssignableFrom(Types.getRawType(oldMethod.returnType))) {
          // The new method is overridden
          continue;
        }
      }

      @SuppressWarnings("unchecked")
      Translator<Object>[] parameterTranslators = (Translator<Object>[]) new Translator<?>[parameterTypeSize];
      for (int i = 0; i < parameterTypeSize; i++) {
        parameterTranslators[i] = depot.getTranslator(parameterTypes[i]);
      }

      methods.put(name, new Method(returnType, returnTranslator, name, parameterTypes, parameterTranslators));
    }

    return methods;
  }

  public static final Factory FACTORY = (depot, type) -> {
    if (!Types.isNonNull(type)) return null;

    Map<String, Method> methods = getInterfaceMethods(depot, type);
    if (methods == null) return null;

    return new InterfaceTranslator(Types.getRawType(type), methods);
  };

  private final Class<?> rawType;
  private final Map<String, Method> methods;

  private InterfaceTranslator(Class<?> rawType, Map<String, Method> methods) {
    super(PICKLE_COMMAND, UNPICKLE_COMMAND);
    this.rawType = rawType;
    this.methods = methods;
  }

  @Override
  protected Object unpickle(JSContext context, BitSource source) {
    long valuePtr = source.readPtr();

    InterfaceInvocationHandler handler = new InterfaceInvocationHandler(context, methods, valuePtr);
    return Proxy.newProxyInstance(
        rawType.getClassLoader(),
        new Class<?>[]{ rawType, JSValueHolder.class, Closeable.class },
        handler
    );
  }

  @Override
  protected void pickle(JSContext context, Object value, BitSink sink) {
    throw new IllegalStateException("TODO");
  }

  private static class InterfaceInvocationHandler implements InvocationHandler {

    private final JSContext context;
    private final Map<String, Method> methods;
    private long pointer;

    InterfaceInvocationHandler(JSContext context, Map<String, Method> methods, long pointer) {
      this.context = context;
      this.methods = methods;
      this.pointer = pointer;

      context.registerJSValue(this, pointer);
    }

    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
      throw new IllegalStateException("TODO");
    }
  }

  private final static class Method {

    final Type returnType;
    final Translator<Object> returnTranslator;
    final String name;
    final Type[] parameterTypes;
    final Translator<Object>[] parameterTranslators;

    private Method(
        Type returnType,
        Translator<Object> returnTranslator,
        String name,
        Type[] parameterTypes,
        Translator<Object>[] parameterTranslators
    ) {
      this.returnType = canonicalize(returnType);
      this.returnTranslator = returnTranslator;
      this.name = name;
      this.parameterTypes = new Type[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        this.parameterTypes[i] = canonicalize(parameterTypes[i]);
      }
      this.parameterTranslators = parameterTranslators;
    }

    private static Type canonicalize(Type type) {
      return Types.removeSubtypeWildcard(Types.canonicalize(type));
    }
  }

  private interface JSValueHolder { long getJSValue(JSValueHolderTag tag); }
  private static class JSValueHolderTag { }
  private static final JSValueHolderTag JS_VALUE_HOLDER_TAG = new JSValueHolderTag();
}
