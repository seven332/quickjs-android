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

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class InterfaceTypeAdapter extends TypeAdapter<Object> {

  /**
   * Returns all methods in the interface type.
   * Returns {@code null} if the type is not interface,
   * or any method is overloaded, or any type can't be resolved.
   */
  @Nullable
  static Map<String, Method> getInterfaceMethods(Type type) {
    Class<?> rawType = Types.getRawType(type);
    if (!rawType.isInterface()) return null;

    Map<String, Method> methods = new HashMap<>();

    for (java.lang.reflect.Method method : rawType.getMethods()) {
      Type returnType = Types.resolve(type, rawType, method.getGenericReturnType());
      // It's not resolved
      if (returnType instanceof TypeVariable) return null;

      String name = method.getName();

      Type[] originParameterTypes = method.getGenericParameterTypes();
      Type[] parameterTypes = new Type[originParameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        parameterTypes[i] = Types.resolve(type, rawType, originParameterTypes[i]);
        // It's not resolved
        if (parameterTypes[i] instanceof TypeVariable) return null;
      }

      Method oldMethod = methods.get(name);
      if (oldMethod != null) {
        if (!Arrays.equals(oldMethod.parameterTypes, parameterTypes)) {
          // overload is not supported
          return null;
        }
        if (returnType.equals(oldMethod.returnType)
            || Types.getRawType(returnType).isAssignableFrom(Types.getRawType(oldMethod.returnType))) {
          // The new method is overridden
          continue;
        }
      }

      methods.put(name, new Method(returnType, name, parameterTypes));
    }

    return methods;
  }

  static final Factory FACTORY = (depot, type) -> {
    Map<String, Method> methods = getInterfaceMethods(type);
    if (methods == null) return null;
    return new InterfaceTypeAdapter(Types.getRawType(type), methods).nullable();
  };

  private final Class<?> rawType;
  private final Map<String, Method> methods;

  private InterfaceTypeAdapter(Class<?> rawType, Map<String, Method> methods) {
    this.rawType = rawType;
    this.methods = methods;
  }

  @Override
  public JSValue toJSValue(Depot depot, Context context, Object value) {
    throw new IllegalStateException("TODO");
  }

  @Override
  public Object fromJSValue(Depot depot, Context context, JSValue value) {
    JSObject jo = value.cast(JSObject.class);

    return Proxy.newProxyInstance(rawType.getClassLoader(), new Class<?>[]{ rawType }, (proxy, method, args) -> {
      // If the method is a method from Object then defer to normal invocation.
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }

      String name = method.getName();
      Method simpleMethod = methods.get(name);
      if (simpleMethod == null) throw new NoSuchMethodException("Can't find method: " + name);

      int parameterNumber = args != null ? args.length : 0;
      if (parameterNumber != simpleMethod.parameterTypes.length) throw new IllegalStateException("Parameter number doesn't match: " + name);
      JSValue[] parameters = new JSValue[parameterNumber];
      for (int i = 0; i < parameterNumber; i++) {
        Type type = simpleMethod.parameterTypes[i];
        TypeAdapter adapter = depot.getAdapter(type);
        parameters[i] = adapter.toJSValue(depot, context, args[i]);
      }

      Type resultType = simpleMethod.returnType;
      TypeAdapter resultAdapter = depot.getAdapter(resultType);

      JSFunction function = jo.getProperty(name).cast(JSFunction.class);

      JSValue result = function.invoke(jo, parameters);

      return resultAdapter.fromJSValue(depot, context, result);
    });
  }
}
