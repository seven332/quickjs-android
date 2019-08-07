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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class InterfaceTypeAdapter extends TypeAdapter<Object> {

  static class SimpleMethod {
    final Type returnType;
    final String name;
    final Type[] parameterTypes;

    SimpleMethod(Type returnType, String name, Type[] parameterTypes) {
      this.returnType = returnType;
      this.name = name;
      this.parameterTypes = parameterTypes;
    }

    @NonNull
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(returnType);
      sb.append(" ");
      sb.append(name);
      sb.append("(");
      for (int i = 0; i < parameterTypes.length; i++) {
        if (i != 0) sb.append(", ");
        sb.append(parameterTypes[i]);
      }
      sb.append(")");
      return sb.toString();
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + returnType.hashCode();
      result = 31 * result + name.hashCode();
      result = 31 * result + Arrays.hashCode(parameterTypes);
      return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (!(obj instanceof SimpleMethod)) return false;
      SimpleMethod other = (SimpleMethod) obj;
      return returnType.equals(other.returnType)
          && name.equals(other.name)
          && Arrays.equals(parameterTypes, other.parameterTypes);
    }
  }

  /**
   * Returns all methods in the interface type.
   * Returns {@code null} if the type is not interface,
   * or any method is overloaded, or any type can't be resolved.
   */
  @Nullable
  static Map<String, SimpleMethod> getInterfaceMethods(Type type) {
    Class<?> rawType = Types.getRawType(type);
    if (!rawType.isInterface()) return null;

    Map<String, SimpleMethod> methods = new HashMap<>();

    for (Method method : rawType.getMethods()) {
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

      SimpleMethod oldMethod = methods.get(name);
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

      methods.put(name, new SimpleMethod(returnType, name, parameterTypes));
    }

    return methods;
  }

  static final Factory FACTORY = (depot, type) -> {
    Map<String, SimpleMethod> methods = getInterfaceMethods(type);
    if (methods == null) return null;
    return new InterfaceTypeAdapter(Types.getRawType(type), methods);
  };

  private final Class<?> rawType;
  private final Map<String, SimpleMethod> methods;

  private InterfaceTypeAdapter(Class<?> rawType, Map<String, SimpleMethod> methods) {
    this.rawType = rawType;
    this.methods = methods;
  }

  @Override
  public JSValue toJSValue(Depot depot, Context context, Object value) {
    return null;
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
      SimpleMethod simpleMethod = methods.get(name);
      if (simpleMethod == null) throw new NoSuchMethodException("Can't find method: " + name);

      int parameterNumber = args != null ? args.length : 0;
      if (parameterNumber != simpleMethod.parameterTypes.length) throw new IllegalStateException("Parameter number doesn't match: " + name);
      JSValue[] parameters = new JSValue[parameterNumber];
      for (int i = 0; i < parameterNumber; i++) {
        Type type = simpleMethod.parameterTypes[i];
        TypeAdapter adapter = depot.getAdapter(type);
        if (adapter == null) throw new IllegalStateException("Can't find TypeAdapter for " + type);
        parameters[i] = adapter.toJSValue(depot, context, args[i]);
      }

      Type resultType = simpleMethod.returnType;
      TypeAdapter resultAdapter = depot.getAdapter(resultType);
      if (resultAdapter == null) throw new IllegalStateException("Can't find TypeAdapter for " + resultType);

      JSFunction function = jo.getProperty(name).cast(JSFunction.class);

      JSValue result = function.invoke(jo, parameters);

      return resultAdapter.fromJSValue(depot, context, result);
    });
  }
}
