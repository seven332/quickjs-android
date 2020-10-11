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

import org.assertj.core.data.MapEntry;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class InterfaceTypeAdapterTest extends TestsWithContext {
  @Test
  public void getInterfaceMethods() {
    Map<String, Method> methods = InterfaceTypeAdapter.getInterfaceMethods(InterfaceC.class);

    assertThat(methods).containsOnly(MapEntry.entry("getValue",
        new Method(NullPointerException.class, "getValue", new Type[]{})),
      MapEntry.entry("setValue",
        new Method(void.class, "setValue", new Type[]{ Throwable.class })),
      MapEntry.entry("setValueResolve",
        new Method(void.class, "setValueResolve", new Type[]{ Throwable.class })),
      MapEntry.entry("fun1",
        new Method(void.class, "fun1", new Type[]{})),
      MapEntry.entry("fun2",
        new Method(String.class, "fun2", new Type[]{ String[].class }))
    );
  }

  @Test
  public void getInterfaceMethods_generic_null() {
    assertThat(InterfaceTypeAdapter.getInterfaceMethods(InterfaceD.class)).isNull();
  }

  @Test
  public void getInterfaceMethods_overload_null() {
    assertThat(InterfaceTypeAdapter.getInterfaceMethods(InterfaceE.class)).isNull();
  }

  private interface InterfaceA<T> {
    T getValue();
    void setValue(T value);
    void setValueResolve(T value);
    void fun1();
    String fun2(String... args);
  }

  private interface InterfaceB {
    RuntimeException getValue();
  }

  private interface InterfaceC extends InterfaceA<Throwable>, InterfaceB {
    @Override
    NullPointerException getValue();
    @Override
    void setValue(Throwable value);
  }

  private interface InterfaceD {
    <T> T fun();
  }

  private interface InterfaceE {
    void fun();
    void fun(int value);
  }

  private static class CalculatorImpl implements Calculator {
    @Override
    public double plus(double a, double b) { return a + b; }
    @Override
    public double minus(double a, double b) { return a - b; }
    @Override
    public double multiplies(double a, double b) { return a * b; }
    @Override
    public double divides(double a, double b) { return a / b; }
    @Override
    public void noop() { }
  }

  @Test
  public void toJSValue() {
    JSValue calculator = quickJS.getAdapter(Calculator.class)
      .toJSValue(quickJS, context, new CalculatorImpl());
    context.getGlobalObject().setProperty("calculator", calculator);

    double a = 3243.435;
    double b = -6541.34;

    assertEquals(a + b, context.evaluate("calculator.plus(" + a + ", " + b + ")", "test.js", double.class), 0.0);
    assertEquals(a - b, context.evaluate("calculator.minus(" + a + ", " + b + ")", "test.js", double.class), 0.0);
    assertEquals(a * b, context.evaluate("calculator.multiplies(" + a + ", " + b + ")", "test.js", double.class), 0.0);
    assertEquals(a / b, context.evaluate("calculator.divides(" + a + ", " + b + ")", "test.js", double.class), 0.0);
  }

  @Test
  public void fromJSValue() {
    Calculator calculator = context.evaluate("" +
      "a = {\n" +
      "  plus: function(a, b) { return a + b },\n" +
      "  minus: function(a, b) { return a - b },\n" +
      "  multiplies: function(a, b) { return a * b },\n" +
      "  divides: function(a, b) { return a / b },\n" +
      "  noop: function() { }\n" +
      "}", "test.js", Calculator.class);

    double a = 3243.435;
    double b = -6541.34;

    assertEquals(a + b, calculator.plus(a, b), 0.0);
    assertEquals(a - b, calculator.minus(a, b), 0.0);
    assertEquals(a * b, calculator.multiplies(a, b), 0.0);
    assertEquals(a / b, calculator.divides(a, b), 0.0);
    calculator.noop();
  }

  @Test
  public void fromJSValue_notJSObject_error() {
    Utils.assertException(
      JSDataException.class,
      "expected: JSObject, actual: JSInt",
      () -> context.evaluate("1", "test.js", Calculator.class)
    );
  }

  @Test
  public void fromJSValue_notJSFunction_error() {
    Calculator calculator = context.evaluate("a = {}", "test.js", Calculator.class);
    Utils.assertException(
      JSDataException.class,
      "expected: JSFunction, actual: JSUndefined",
      () -> calculator.plus(0, 0)
    );
  }

  @Test
  public void fromJSValue_unknownType_error() {
    AtomicIntegerHolder holder = context.evaluate("" +
      "a = {\n" +
      "  get: function() { return 0 }\n" +
      "}", "test.js", AtomicIntegerHolder.class);
    Utils.assertException(
      IllegalArgumentException.class,
      "Can't find TypeAdapter for class java.util.concurrent.atomic.AtomicInteger",
      holder::get
    );
  }

  interface Calculator {
    double plus(double a, double b);
    double minus(double a, double b);
    double multiplies(double a, double b);
    double divides(double a, double b);
    void noop();
  }

  interface AtomicIntegerHolder {
    AtomicInteger get();
  }

  @Test
  public void fromJSValueThenToJSValue_isSame() {
    JSValue jsValue1 = context.evaluate("" +
      "a = {\n" +
      "  plus: function(a, b) { return a + b },\n" +
      "  minus: function(a, b) { return a - b },\n" +
      "  multiplies: function(a, b) { return a * b },\n" +
      "  divides: function(a, b) { return a / b },\n" +
      "  noop: function() { }\n" +
      "}", "test.js", JSValue.class);

    TypeAdapter<Calculator> adapter = quickJS.getAdapter(Calculator.class);
    Calculator calculator = adapter.fromJSValue(quickJS, context, jsValue1);
    JSValue jsValue2 = adapter.toJSValue(quickJS, context, calculator);
    assertSame(jsValue1, jsValue2);
  }

  @Test
  public void toJSValueThenFromJSValue_isSame() {
    Calculator calculator1 = new CalculatorImpl();
    TypeAdapter<Calculator> adapter = quickJS.getAdapter(Calculator.class);
    JSValue jsValue = adapter.toJSValue(quickJS, context, calculator1);
    Calculator calculator2 = adapter.fromJSValue(quickJS, context, jsValue);
    assertSame(calculator1, calculator2);
  }
}
