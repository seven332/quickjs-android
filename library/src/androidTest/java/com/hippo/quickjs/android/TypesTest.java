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
import java.lang.reflect.WildcardType;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public final class TypesTest {

  @Test
  public void newParameterizedType() {
    // List<A>. List is a top-level class.
    Type type = Types.newParameterizedType(List.class, A.class);
    assertThat(getFirstTypeArgument(type)).isEqualTo(A.class);

    // A<B>. A is a static inner class.
    type = Types.newParameterizedTypeWithOwner(TypesTest.class, A.class, B.class);
    assertThat(getFirstTypeArgument(type)).isEqualTo(B.class);
  }

  @Test
  public void parameterizedTypeWithRequiredOwnerMissing() {
    Utils.assertException(
        IllegalArgumentException.class,
        "unexpected owner type for " + A.class + ": null",
        () -> Types.newParameterizedType(A.class, B.class)
    );
  }

  @Test
  public void parameterizedTypeWithUnnecessaryOwnerProvided() {
    Utils.assertException(
        IllegalArgumentException.class,
        "unexpected owner type for " + List.class + ": " + A.class,
        () -> Types.newParameterizedTypeWithOwner(A.class, List.class, B.class)
    );
  }

  @Test
  public void parameterizedTypeWithIncorrectOwnerProvided() {
    Utils.assertException(
        IllegalArgumentException.class,
        "unexpected owner type for " + D.class + ": " + A.class,
        () -> Types.newParameterizedTypeWithOwner(A.class, D.class, B.class)
    );
  }

  @Test
  public void arrayOf() {
    assertThat(Types.getRawType(Types.arrayOf(int.class))).isEqualTo(int[].class);
    assertThat(Types.getRawType(Types.arrayOf(List.class))).isEqualTo(List[].class);
    assertThat(Types.getRawType(Types.arrayOf(String[].class))).isEqualTo(String[][].class);
  }

  List<? extends CharSequence> listSubtype;
  List<? super String> listSupertype;

  private static Type collectionElementType(Type context) {
    Type collectionType = Types.getSupertype(context, Collection.class);

    if (collectionType instanceof WildcardType) {
      collectionType = ((WildcardType) collectionType).getUpperBounds()[0];
    }
    if (collectionType instanceof ParameterizedType) {
      return ((ParameterizedType) collectionType).getActualTypeArguments()[0];
    }
    return Object.class;
  }

  @Test
  public void subtypeOf() throws Exception {
    Type listOfWildcardType = TypesTest.class.getDeclaredField("listSubtype").getGenericType();
    Type expected = collectionElementType(listOfWildcardType);
    Type subtype = Types.subtypeOf(CharSequence.class);
    assertThat(subtype).isEqualTo(expected);
    assertThat(subtype.hashCode()).isEqualTo(expected.hashCode());
    assertThat(subtype.toString()).isEqualTo(expected.toString());
  }

  @Test
  public void supertypeOf() throws Exception {
    Type listOfWildcardType = TypesTest.class.getDeclaredField("listSupertype").getGenericType();
    Type expected = collectionElementType(listOfWildcardType);
    Type supertype = Types.supertypeOf(String.class);
    assertThat(supertype).isEqualTo(expected);
    assertThat(supertype.hashCode()).isEqualTo(expected.hashCode());
    assertThat(supertype.toString()).isEqualTo(expected.toString());
  }

  @Test
  public void getFirstTypeArgument() throws Exception {
    assertThat(getFirstTypeArgument(A.class)).isNull();

    Type type = Types.newParameterizedTypeWithOwner(TypesTest.class, A.class, B.class, C.class);
    assertThat(getFirstTypeArgument(type)).isEqualTo(B.class);
  }

  @Test
  public void newParameterizedTypeObjectMethods() throws Exception {
    Type mapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "mapOfStringInteger").getGenericType();
    ParameterizedType newMapType = Types.newParameterizedType(Map.class, String.class, Integer.class);
    assertThat(newMapType).isEqualTo(mapOfStringIntegerType);
    assertThat(newMapType.hashCode()).isEqualTo(mapOfStringIntegerType.hashCode());
    assertThat(newMapType.toString()).isEqualTo(mapOfStringIntegerType.toString());

    Type arrayListOfMapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "arrayListOfMapOfStringInteger").getGenericType();
    ParameterizedType newListType = Types.newParameterizedType(ArrayList.class, newMapType);
    assertThat(newListType).isEqualTo(arrayListOfMapOfStringIntegerType);
    assertThat(newListType.hashCode()).isEqualTo(arrayListOfMapOfStringIntegerType.hashCode());
    assertThat(newListType.toString()).isEqualTo(arrayListOfMapOfStringIntegerType.toString());
  }

  @Test
  public void getGenericSuperclass() throws Exception {
    Type stringAtomicReference = TypesTest.class.getDeclaredField(
        "stringAtomicReference").getGenericType();
    Type genericSuperclass = Types.getGenericSuperclass(StringAtomicReference.class);
    assertThat(genericSuperclass).isEqualTo(stringAtomicReference);
    assertThat(genericSuperclass.hashCode()).isEqualTo(stringAtomicReference.hashCode());
    assertThat(genericSuperclass.toString()).isEqualTo(stringAtomicReference.toString());
  }

  @Test
  public void getGenericInterfaces() throws Exception {
    Type mapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "mapOfStringInteger").getGenericType();
    Type[] genericInterfaces = Types.getGenericInterfaces(StringIntegerMap.class);
    assertThat(genericInterfaces).hasSize(1);
    Type genericInterface = genericInterfaces[0];
    assertThat(genericInterface).isEqualTo(mapOfStringIntegerType);
    assertThat(genericInterface.hashCode()).isEqualTo(mapOfStringIntegerType.hashCode());
    assertThat(genericInterface.toString()).isEqualTo(mapOfStringIntegerType.toString());
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
    return Types.canonicalize(actualTypeArguments[0]);
  }

  Map<String, Integer> mapOfStringInteger;
  Map<String, Integer>[] arrayOfMapOfStringInteger;
  ArrayList<Map<String, Integer>> arrayListOfMapOfStringInteger;
  interface StringIntegerMap extends Map<String, Integer> {
  }

  AtomicReference<String> stringAtomicReference;
  class StringAtomicReference extends AtomicReference<String> {
  }

  @Test
  public void arrayComponentType() throws Exception {
    assertThat(Types.arrayComponentType(String[][].class)).isEqualTo(String[].class);
    assertThat(Types.arrayComponentType(String[].class)).isEqualTo(String.class);

    Type arrayOfMapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "arrayOfMapOfStringInteger").getGenericType();
    Type mapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "mapOfStringInteger").getGenericType();
    assertThat(Types.arrayComponentType(arrayOfMapOfStringIntegerType))
        .isEqualTo(mapOfStringIntegerType);
  }

  @Test
  public void collectionElementType() throws Exception {
    Type arrayListOfMapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "arrayListOfMapOfStringInteger").getGenericType();
    Type mapOfStringIntegerType = TypesTest.class.getDeclaredField(
        "mapOfStringInteger").getGenericType();
    assertThat(collectionElementType(arrayListOfMapOfStringIntegerType))
        .isEqualTo(mapOfStringIntegerType);
  }

  @Test
  public void arrayEqualsGenericTypeArray() {
    assertThat(Types.equals(int[].class, Types.arrayOf(int.class))).isTrue();
    assertThat(Types.equals(Types.arrayOf(int.class), int[].class)).isTrue();
    assertThat(Types.equals(String[].class, Types.arrayOf(String.class))).isTrue();
    assertThat(Types.equals(Types.arrayOf(String.class), String[].class)).isTrue();
  }

  @Test
  public void parameterizedAndWildcardTypesCannotHavePrimitiveArguments() {
    Utils.assertException(
        IllegalArgumentException.class,
        "Unexpected primitive int. Use the boxed type.",
        () -> Types.newParameterizedType(List.class, int.class)
    );

    Utils.assertException(
        IllegalArgumentException.class,
        "Unexpected primitive byte. Use the boxed type.",
        () -> Types.subtypeOf(byte.class)
    );

    Utils.assertException(
        IllegalArgumentException.class,
        "Unexpected primitive boolean. Use the boxed type.",
        () -> Types.subtypeOf(boolean.class)
    );
  }

  @Test
  public void getRawTypeForNonNull() {
    assertThat(Types.getRawType(Types.nonNullOf(List.class)))
        .isEqualTo(List.class);
    assertThat(Types.getRawType(Types.nonNullOf(Types.newParameterizedType(List.class, String.class))))
        .isEqualTo(List.class);
    assertThat(Types.getRawType(Types.nonNullOf(Types.newParameterizedType(List.class, Types.nonNullOf(String.class)))))
        .isEqualTo(List.class);
  }

  @Test
  public void equalsForNonNull() {
    assertThat(Types.equals(Types.nonNullOf(List.class), Types.nonNullOf(List.class))).isTrue();
    assertThat(Types.equals(Types.nonNullOf(List.class), Types.nonNullOf(Set.class))).isFalse();
    assertThat(Types.equals(Types.nonNullOf(List.class), List.class)).isFalse();
    assertThat(Types.equals(Types.nonNullOf(List.class), Set.class)).isFalse();
    assertThat(Types.equals(Types.nonNullOf(List.class), null)).isFalse();
  }

  @Test
  public void getSupertypeForNonNull() throws NoSuchFieldException {
    Type listOfWildcardType = TypesTest.class.getDeclaredField("listSubtype").getGenericType();
    Type nonNullListOfWildcardType = Types.nonNullOf(listOfWildcardType);
    assertThat(Types.getSupertype(nonNullListOfWildcardType, Iterable.class))
        .isEqualTo(Types.nonNullOf(Types.newParameterizedType(Iterable.class, Types.subtypeOf(CharSequence.class))));
  }

  @Test
  public void getGenericSuperclassForNonNull() {
    Type type = Types.nonNullOf(Types.newParameterizedType(ArrayList.class, String.class));
    assertThat(Types.getGenericSuperclass(type))
        .isEqualTo(Types.nonNullOf(Types.newParameterizedType(AbstractList.class, String.class)));
  }

  @Test
  public void getGenericInterfacesForNonNull() {
    Type type = Types.nonNullOf(Types.newParameterizedType(List.class, String.class));
    assertThat(Types.getGenericInterfaces(type))
        .isEqualTo(new Type[] { Types.nonNullOf(Types.newParameterizedType(Collection.class, String.class)) });
  }

  @Test
  public void arrayComponentTypeForNonNull() {
    Type type = Types.nonNullOf(Types.arrayOf(String.class));
    assertThat(Types.arrayComponentType(type))
        .isEqualTo(String.class);
  }
}
