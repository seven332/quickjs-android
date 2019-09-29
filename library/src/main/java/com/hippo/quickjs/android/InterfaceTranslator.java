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
import java.lang.reflect.Method;
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
  static Map<String, RichMethod> getInterfaceRichMethods(Translator.Depot depot, Type type) {
    Class<?> rawType = Types.getRawType(type);
    if (!rawType.isInterface()) return null;

    Map<String, RichMethod> richMethods = new HashMap<>();

    for (Method method : rawType.getMethods()) {
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

      // close() must be from Closeable
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

      RichMethod oldRichMethod = richMethods.get(name);
      if (oldRichMethod != null) {
        if (!typeArrayEquals(oldRichMethod.parameterTypes, parameterTypes)) {
          throw new IllegalArgumentException("Overload is not supported");
        }
        if (Types.equals(returnType, oldRichMethod.returnType)
            || Types.getRawType(returnType).isAssignableFrom(Types.getRawType(oldRichMethod.returnType))) {
          // The new method is overridden
          continue;
        }
      }

      @SuppressWarnings("unchecked")
      Translator<Object>[] parameterTranslators = (Translator<Object>[]) new Translator<?>[parameterTypeSize];
      for (int i = 0; i < parameterTypeSize; i++) {
        parameterTranslators[i] = depot.getTranslator(parameterTypes[i]);
      }

      richMethods.put(name, new RichMethod(returnType, returnTranslator, name, parameterTypes, parameterTranslators));
    }

    return richMethods;
  }

  public static final Factory FACTORY = (depot, type) -> {
    if (!Types.isNonNull(type)) return null;

    Map<String, RichMethod> richMethods = getInterfaceRichMethods(depot, type);
    if (richMethods == null) return null;

    return new InterfaceTranslator(Types.getRawType(type), richMethods);
  };

  private final Class<?> rawType;
  private final Map<String, RichMethod> richMethods;

  private InterfaceTranslator(Class<?> rawType, Map<String, RichMethod> richMethods) {
    super(PICKLE_COMMAND, UNPICKLE_COMMAND);
    this.rawType = rawType;
    this.richMethods = richMethods;
  }

  @Override
  protected Object unpickle(JSContext context, BitSource source) {
    long valuePtr = source.readPtr();

    InterfaceInvocationHandler handler = new InterfaceInvocationHandler(context, richMethods, valuePtr);
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
    private final Map<String, RichMethod> richMethods;
    private long pointer;

    InterfaceInvocationHandler(JSContext context, Map<String, RichMethod> richMethods, long pointer) {
      this.context = context;
      this.richMethods = richMethods;
      this.pointer = pointer;

      context.registerJSValue(this, pointer);
    }

    private void checkClosed() {
      if (pointer == 0) throw new IllegalStateException("The JSValue is closed");
    }

    private Object invokeMethod(RichMethod richMethod, Object[] args, boolean required) {
      synchronized (context.jsRuntime) {
        checkClosed();

        int length = richMethod.parameterTypes.length;
        long[] unpicklePointers = new long[length];
        byte[][] argContexts = new byte[length][];
        int[] argContextSizes = new int[length];
        for (int i = 0; i < length; i++) {
          Translator<Object> translator = richMethod.parameterTranslators[i];
          unpicklePointers[i] = translator.unpicklePointer;
          BitSink sink = translator.pickle(context, args[i]);
          argContexts[i] = sink.getBytes();
          argContextSizes[i] = sink.getSize();
        }

        Translator<Object> translator = richMethod.returnTranslator;
        byte[] bytes = QuickJS.invokeValueFunction(context.pointer, pointer, richMethod.name,
            unpicklePointers, argContexts, argContextSizes, translator.picklePointer, required);
        if (bytes != null) {
          return translator.unpickle(context, bytes);
        } else {
          return null;
        }
      }
    }

    @Override
    public Object invoke(Object proxy, Method method, @Nullable Object[] args) throws Throwable {
      // If the method is a method from Object then defer to normal invocation.
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }

      String name = method.getName();

      // close()
      if ("close".equals(name)) {
        close();
        return null;
      }

      // getJSValue()
      if (args != null && args.length == 1 && args[0] == JS_VALUE_HOLDER_TAG) {
        return getJSValue();
      }

      RichMethod richMethod = richMethods.get(name);
      if (richMethod == null) throw new RuntimeException("Can't find method: " + name);

      return invokeMethod(richMethod, args != null ? args : EMPTY_ARGS, true);
    }

    public long getJSValue() {
      synchronized (context.jsRuntime) {
        checkClosed();
        return pointer;
      }
    }

    public void close() {
      synchronized (context.jsRuntime) {
        if (pointer != 0) {
          try {
            RichMethod richMethod = richMethods.get("close");
            if (richMethod != null) invokeMethod(richMethod, EMPTY_ARGS, false);
          } finally {
            long valueToClose = pointer;
            pointer = 0;
            QuickJS.destroyValue(context.pointer, valueToClose);
            context.unregisterJSValue(valueToClose);
          }
        }
      }
    }
  }

  private final static class RichMethod {

    final Type returnType;
    final Translator<Object> returnTranslator;
    final String name;
    final Type[] parameterTypes;
    final Translator<Object>[] parameterTranslators;

    private RichMethod(
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

  private interface JSValueHolder { long getJSValue(Object tag); }
  private static final Object JS_VALUE_HOLDER_TAG = new Object();
}
