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
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class JSContextTest {

  @Ignore("There is no guarantee that this test will pass")
  @Test
  public void testJSValueGC() {
    QuickJS quickJS = new QuickJS.Builder().build();

    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        int jsValueCount = 3;

        assertEquals(0, context.getNotRemovedJSValueCount());

        for (int i = 0; i < jsValueCount; i++) {
          context.evaluate("1", "unknown.js", int.class);
        }

        assertEquals(jsValueCount, context.getNotRemovedJSValueCount());

        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();

        context.evaluate("1", "unknown.js", int.class);

        assertEquals(1, context.getNotRemovedJSValueCount());
      }
    }
  }

  @Test
  public void throwException() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertException(
            JSEvaluationException.class,
            "Throw: 1\n",
            () -> context.evaluate("throw 1", "unknown.js")
        );
      }
    }
  }

  @Test
  public void throwExceptionWithReturn() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        assertException(
            JSEvaluationException.class,
            "Throw: 1\n",
            () -> context.evaluate("throw 1", "unknown.js", int.class)
        );
      }
    }
  }

  @Test
  public void getGlobalObject() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        context.evaluate("a = 1", "unknown.js", int.class);
        assertEquals(1, context.getGlobalObject().getProperty("a").cast(JSNumber.class).getInt());

        context.getGlobalObject().setProperty("b", context.createJSString("string"));
        assertEquals("string", context.evaluate("b", "unknown.js", String.class));
      }
    }
  }

  @Test
  public void evaluateWithoutReturn() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        context.evaluate("a = {}", "test.js");
        assertThat(context.getGlobalObject().getProperty("a")).isInstanceOf(JSObject.class);
      }
    }
  }

  private static class StringHolder {
    final String str;

    StringHolder(String str) {
      this.str = str;
    }

    @Override
    public int hashCode() {
      return str.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof StringHolder && str.equals(((StringHolder) obj).str);
    }
  }

  private static class ClassA {
    boolean emptyCalled;
    public void funEmpty() { emptyCalled = false; }

    public boolean funBoolean(boolean a, Boolean b) { return a && b; }
    public char funChar(char a, Character b) { return (char) ((short) a + (short) (char) b); }
    public byte funByte(byte a, Byte b) { return (byte) (a + b); }
    public short funShort(short a, Short b) { return (short) (a + b); }
    public int funInt(int a, Integer b) { return a + b; }
    public long funLong(long a, Long b) { return a + b; }
    public float funFloat(float a, Float b) { return a + b; }
    public double funDouble(double a, Double b) { return a + b; }
    public String funString(char a, String b) { return a + b; }
    public StringHolder funInnerClass(char a, StringHolder b) { return new StringHolder(a + b.str); }

    static boolean staticEmptyCalled;
    public static void staticFunEmpty() { staticEmptyCalled = false; }

    public static boolean staticFunBoolean(boolean a, Boolean b) { return a && b; }
    public static char staticFunChar(char a, Character b) { return (char) ((short) a + (short) (char) b); }
    public static byte staticFunByte(byte a, Byte b) { return (byte) (a + b); }
    public static short staticFunShort(short a, Short b) { return (short) (a + b); }
    public static int staticFunInt(int a, Integer b) { return a + b; }
    public static long staticFunLong(long a, Long b) { return a + b; }
    public static float staticFunFloat(float a, Float b) { return a + b; }
    public static double staticFunDouble(double a, Double b) { return a + b; }
    public static String staticFunString(char a, String b) { return a + b; }
    public static StringHolder staticFunInnerClass(char a, StringHolder b) { return new StringHolder(a + b.str); }
  }

  private void invokeJavaMethodInJS(
      JSContext context,
      Object instance,
      java.lang.reflect.Method rawMethod,
      Object[] args
  ) throws InvocationTargetException, IllegalAccessException {
    Method method = Method.create(instance.getClass(), rawMethod);
    assertNotNull(method);

    JSFunction fun = context.createJSFunction(instance, method);
    context.getGlobalObject().setProperty("fun", fun);

    JSValue[] jsArgs = new JSValue[method.parameterTypes.length];
    for (int i = 0; i < method.parameterTypes.length; i++) {
      jsArgs[i] = context.quickJS.getAdapter(method.parameterTypes[i]).toJSValue(context.quickJS, context, args[i]);
    }

    JSValue jsResult = fun.invoke(null, jsArgs);

    assertEquals(
        rawMethod.invoke(instance, args),
        context.quickJS.getAdapter(method.returnType).fromJSValue(context.quickJS, context, jsResult)
    );
  }

  private void invokeJavaStaticMethodInJS(
      JSContext context,
      Class clazz,
      java.lang.reflect.Method rawMethod,
      Object[] args
  ) throws InvocationTargetException, IllegalAccessException {
    Method method = Method.create(clazz, rawMethod);
    assertNotNull(method);

    JSFunction fun = context.createJSFunctionS(clazz, method);
    context.getGlobalObject().setProperty("fun", fun);

    JSValue[] jsArgs = new JSValue[method.parameterTypes.length];
    for (int i = 0; i < method.parameterTypes.length; i++) {
      jsArgs[i] = context.quickJS.getAdapter(method.parameterTypes[i]).toJSValue(context.quickJS, context, args[i]);
    }

    JSValue jsResult = fun.invoke(null, jsArgs);

    assertEquals(
        rawMethod.invoke(null, args),
        context.quickJS.getAdapter(method.returnType).fromJSValue(context.quickJS, context, jsResult)
    );
  }

  @Test
  public void createValueFunction() throws InvocationTargetException, IllegalAccessException {
    QuickJS quickJS = new QuickJS.Builder().registerTypeAdapter(StringHolder.class, new TypeAdapter<StringHolder>() {
      @Override
      public JSValue toJSValue(Depot depot, Context context, @Nullable StringHolder value) {
        return context.createJSString(value.str);
      }
      @Override
      public StringHolder fromJSValue(Depot depot, Context context, JSValue value) {
        return new StringHolder(value.cast(JSString.class).getString());
      }
    }).build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        ClassA a = new ClassA();

        JSFunction fun1 = context.createJSFunction(a, new Method(void.class, "funEmpty", new Type[] {}));
        context.getGlobalObject().setProperty("fun", fun1);
        a.emptyCalled = true;
        context.evaluate("fun()", "test.js");
        assertFalse(a.emptyCalled);

        for (java.lang.reflect.Method rawMethod : ClassA.class.getMethods()) {
          Object[] args = null;
          switch (rawMethod.getName()) {
            case "funBoolean": args = new Object[] { true, false }; break;
            case "funChar": args = new Object[] { 'a', '*' }; break;
            case "funByte": args = new Object[] { (byte) 23, (byte) (-54) }; break;
            case "funShort": args = new Object[] { (short) 23, (short) (-54) }; break;
            case "funInt": args = new Object[] { 23, -54 }; break;
            case "funLong": args = new Object[] { 23L, -54L }; break;
            case "funFloat": args = new Object[] { 23.1f, -54.8f }; break;
            case "funDouble": args = new Object[] { 23.1, -54.8 }; break;
            case "funString": args = new Object[] { '9', "str" }; break;
            case "funInnerClass": args = new Object[] { '9', new StringHolder("str") }; break;
          }
          if (args != null) {
            invokeJavaMethodInJS(context, a, rawMethod, args);
          }
        }

        JSFunction fun2 = context.createJSFunctionS(ClassA.class, new Method(void.class, "staticFunEmpty", new Type[] {}));
        context.getGlobalObject().setProperty("fun", fun2);
        ClassA.staticEmptyCalled = true;
        context.evaluate("fun()", "test.js");
        assertFalse(ClassA.staticEmptyCalled);

        for (java.lang.reflect.Method rawMethod : ClassA.class.getMethods()) {
          Object[] args = null;
          switch (rawMethod.getName()) {
            case "staticFunBoolean": args = new Object[] { true, false }; break;
            case "staticFunChar": args = new Object[] { 'a', '*' }; break;
            case "staticFunByte": args = new Object[] { (byte) 23, (byte) (-54) }; break;
            case "staticFunShort": args = new Object[] { (short) 23, (short) (-54) }; break;
            case "staticFunInt": args = new Object[] { 23, -54 }; break;
            case "staticFunLong": args = new Object[] { 23L, -54L }; break;
            case "staticFunFloat": args = new Object[] { 23.1f, -54.8f }; break;
            case "staticFunDouble": args = new Object[] { 23.1, -54.8 }; break;
            case "staticFunString": args = new Object[] { '9', "str" }; break;
            case "staticFunInnerClass": args = new Object[] { '9', new StringHolder("str") }; break;
          }
          if (args != null) {
            invokeJavaStaticMethodInJS(context, ClassA.class, rawMethod, args);
          }
        }
      }
    }
  }

  @Test
  public void createValueFunctionNoMethod() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        Method method = new Method(long.class, "atoi", new Type[] { String.class });
        assertException(
            NoSuchMethodError.class,
            "no non-static method \"Ljava/lang/Integer;.atoi(Ljava/lang/String;)J\"",
            () -> context.createJSFunction(1, method)
        );
        assertException(
            NoSuchMethodError.class,
            "no static method \"Ljava/lang/Integer;.atoi(Ljava/lang/String;)J\"",
            () -> context.createJSFunctionS(Integer.class, method)
        );
      }
    }
  }

  @Test
  public void createValueFunctionNull() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {
        Method method = new Method(int.class, "atoi", new Type[] { String.class });

        assertException(
            NullPointerException.class,
            "instance == null",
            () -> context.createJSFunction(null, method)
        );

        assertException(
            NullPointerException.class,
            "method == null",
            () -> context.createJSFunction(1, null)
        );

        assertException(
            NullPointerException.class,
            "clazz == null",
            () -> context.createJSFunctionS(null, method)
        );

        assertException(
            NullPointerException.class,
            "method == null",
            () -> context.createJSFunctionS(Class.class, null)
        );
      }
    }
  }
}
