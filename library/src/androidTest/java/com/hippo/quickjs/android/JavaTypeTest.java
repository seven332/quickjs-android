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

import org.junit.Test;

import static org.junit.Assert.*;

public class JavaTypeTest {

  @Test
  public void testPrimitiveClass() {
    Class<?>[] types = { boolean.class, int.class, long.class, float.class, double.class };
    for (Class<?> type : types) {
      JSContext.JavaType javaType = JSContext.JavaType.from(type);
      assertEquals(type, javaType.type);
      assertFalse(javaType.nullable);
      assertNull(javaType.methods);
    }
  }

  @Test
  public void testObjectClass() {
    Class<?>[] types = { Boolean.class, Integer.class, Long.class, Float.class, Double.class, String.class };
    for (Class<?> type : types) {
      JSContext.JavaType javaType = JSContext.JavaType.from(type);
      assertEquals(type, javaType.type);
      assertTrue(javaType.nullable);
      assertNull(javaType.methods);
    }
  }

  @Test
  public void testInterface() {
    JSContext.JavaType javaType = JSContext.JavaType.from(InterfaceA.class);
    assertEquals(InterfaceA.class, javaType.type);
    assertTrue(javaType.nullable);
    assertEquals(1, javaType.methods.size());
  }

  @Test
  public void testInterfaceDifferentReturnType() {
    JSContext.JavaType javaType = JSContext.JavaType.from(InterfaceD.class);
    assertEquals(javaType.type, InterfaceD.class);
    assertTrue(javaType.nullable);
    assertEquals(2, javaType.methods.size());
    assertEquals(String.class, javaType.methods.get("fun1").getReturnType());
  }

  interface InterfaceA {
    CharSequence fun1();
  }

  interface InterfaceB {
    String fun1();
  }

  interface InterfaceC {
    CharSequence fun1();
  }

  interface InterfaceD extends InterfaceA, InterfaceB, InterfaceC {
    void fun2();
  }

  @Test
  public void testInterfaceOverload() {
    try {
      JSContext.JavaType.from(Interface2.class);
      fail();
    } catch (QuickJSException e) {
      assertEquals("fun1 is overloaded in interface com.hippo.quickjs.android.JavaTypeTest$Interface2", e.getMessage());
    }
  }

  interface Interface1 {
    void fun1();
  }

  interface Interface2 extends Interface1 {
    void fun1(String a);
  }
}
