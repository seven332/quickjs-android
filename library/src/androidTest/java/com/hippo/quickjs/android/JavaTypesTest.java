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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.assertj.core.api.Assertions.assertThat;

public final class JavaTypesTest {

  @Test
  public void newParameterizedType() {
    // List<A>. List is a top-level class.
    Type type = JavaTypes.newParameterizedType(List.class, A.class);
    assertThat(getFirstTypeArgument(type)).isEqualTo(A.class);

    // A<B>. A is a static inner class.
    type = JavaTypes.newParameterizedTypeWithOwner(JavaTypesTest.class, A.class, B.class);
    assertThat(getFirstTypeArgument(type)).isEqualTo(B.class);
  }

  @Test
  public void parameterizedTypeWithRequiredOwnerMissing() {
    assertException(
        IllegalArgumentException.class,
        "unexpected owner type for " + A.class + ": null",
        () -> JavaTypes.newParameterizedType(A.class, B.class)
    );
  }

  @Test
  public void parameterizedTypeWithUnnecessaryOwnerProvided() {
    assertException(
        IllegalArgumentException.class,
        "unexpected owner type for " + List.class + ": " + A.class,
        () -> JavaTypes.newParameterizedTypeWithOwner(A.class, List.class, B.class)
    );
  }

  @Test
  public void parameterizedTypeWithIncorrectOwnerProvided() {
    assertException(
        IllegalArgumentException.class,
        "unexpected owner type for " + D.class + ": " + A.class,
        () -> JavaTypes.newParameterizedTypeWithOwner(A.class, D.class, B.class)
    );
  }

  @Test
  public void arrayOf() {
    assertThat(JavaTypes.getRawType(JavaTypes.arrayOf(int.class))).isEqualTo(int[].class);
    assertThat(JavaTypes.getRawType(JavaTypes.arrayOf(List.class))).isEqualTo(List[].class);
    assertThat(JavaTypes.getRawType(JavaTypes.arrayOf(String[].class))).isEqualTo(String[][].class);
  }

  List<? extends CharSequence> listSubtype;
  List<? super String> listSupertype;

  @Test
  public void subtypeOf() throws Exception {
    Type listOfWildcardType = JavaTypesTest.class.getDeclaredField("listSubtype").getGenericType();
    Type expected = ((ParameterizedType) JavaTypes.canonicalize(listOfWildcardType)).getActualTypeArguments()[0];
    Type subtype = JavaTypes.subtypeOf(CharSequence.class);
    assertThat(subtype).isEqualTo(expected);
    assertThat(subtype.hashCode()).isEqualTo(expected.hashCode());
    assertThat(subtype.toString()).isEqualTo(expected.toString());
  }

  @Test
  public void supertypeOf() throws Exception {
    Type listOfWildcardType = JavaTypesTest.class.getDeclaredField("listSupertype").getGenericType();
    Type expected = ((ParameterizedType) JavaTypes.canonicalize(listOfWildcardType)).getActualTypeArguments()[0];
    Type supertype = JavaTypes.supertypeOf(String.class);
    assertThat(supertype).isEqualTo(expected);
    assertThat(supertype.hashCode()).isEqualTo(expected.hashCode());
    assertThat(supertype.toString()).isEqualTo(expected.toString());
  }

  @Test
  public void getFirstTypeArgument() {
    assertThat(getFirstTypeArgument(A.class)).isNull();

    Type type = JavaTypes.newParameterizedTypeWithOwner(JavaTypesTest.class, A.class, B.class, C.class);
    assertThat(getFirstTypeArgument(type)).isEqualTo(B.class);
  }

  @Test
  public void newParameterizedTypeObjectMethods() throws Exception {
    Type mapOfStringIntegerType = JavaTypesTest.class.getDeclaredField(
        "mapOfStringInteger").getGenericType();
    ParameterizedType newMapType = JavaTypes.newParameterizedType(Map.class, String.class, Integer.class);
    assertThat(newMapType).isEqualTo(mapOfStringIntegerType);
    assertThat(newMapType.hashCode()).isEqualTo(mapOfStringIntegerType.hashCode());
    assertThat(newMapType.toString()).isEqualTo(mapOfStringIntegerType.toString());

    Type arrayListOfMapOfStringIntegerType = JavaTypesTest.class.getDeclaredField(
        "arrayListOfMapOfStringInteger").getGenericType();
    ParameterizedType newListType = JavaTypes.newParameterizedType(ArrayList.class, newMapType);
    assertThat(newListType).isEqualTo(arrayListOfMapOfStringIntegerType);
    assertThat(newListType.hashCode()).isEqualTo(arrayListOfMapOfStringIntegerType.hashCode());
    assertThat(newListType.toString()).isEqualTo(arrayListOfMapOfStringIntegerType.toString());
  }

  private static final class A {
  }

  private static final class B {
  }

  private static final class C {
  }

  private static final class D<T> {
  }

  /**
   * Given a parameterized type {@code A<B, C>}, returns B. If the specified type is not a generic
   * type, returns null.
   */
  private static Type getFirstTypeArgument(Type type) {
    if (!(type instanceof ParameterizedType)) return null;
    ParameterizedType ptype = (ParameterizedType) type;
    Type[] actualTypeArguments = ptype.getActualTypeArguments();
    if (actualTypeArguments.length == 0) return null;
    return JavaTypes.canonicalize(actualTypeArguments[0]);
  }

  Map<String, Integer> mapOfStringInteger;
  Map<String, Integer>[] arrayOfMapOfStringInteger;
  ArrayList<Map<String, Integer>> arrayListOfMapOfStringInteger;

  @Test
  public void arrayComponentType() throws Exception {
    assertThat(JavaTypes.arrayComponentType(String[][].class)).isEqualTo(String[].class);
    assertThat(JavaTypes.arrayComponentType(String[].class)).isEqualTo(String.class);

    Type arrayOfMapOfStringIntegerType = JavaTypesTest.class.getDeclaredField(
        "arrayOfMapOfStringInteger").getGenericType();
    Type mapOfStringIntegerType = JavaTypesTest.class.getDeclaredField(
        "mapOfStringInteger").getGenericType();
    assertThat(JavaTypes.arrayComponentType(arrayOfMapOfStringIntegerType))
        .isEqualTo(mapOfStringIntegerType);
  }

  @Test
  public void arrayEqualsGenericTypeArray() {
    assertThat(JavaTypes.equals(int[].class, JavaTypes.arrayOf(int.class))).isTrue();
    assertThat(JavaTypes.equals(JavaTypes.arrayOf(int.class), int[].class)).isTrue();
    assertThat(JavaTypes.equals(String[].class, JavaTypes.arrayOf(String.class))).isTrue();
    assertThat(JavaTypes.equals(JavaTypes.arrayOf(String.class), String[].class)).isTrue();
  }

  @Test
  public void parameterizedAndWildcardTypesCannotHavePrimitiveArguments() {
    assertException(
        IllegalArgumentException.class,
        "Unexpected primitive int. Use the boxed type.",
        () -> JavaTypes.newParameterizedType(List.class, int.class)
    );

    assertException(
        IllegalArgumentException.class,
        "Unexpected primitive byte. Use the boxed type.",
        () -> JavaTypes.subtypeOf(byte.class)
    );

    assertException(
        IllegalArgumentException.class,
        "Unexpected primitive boolean. Use the boxed type.",
        () -> JavaTypes.subtypeOf(boolean.class)
    );
  }
}
