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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

// https://github.com/square/moshi/blob/master/moshi/src/main/java/com/squareup/moshi/Types.java
// https://github.com/square/moshi/blob/master/moshi/src/main/java/com/squareup/moshi/internal/Util.java

class Types {

  private static final Type[] EMPTY_TYPE_ARRAY = new Type[] {};

  private static final Map<Type, Type> UNBOX_MAP = new HashMap<>(8);
  private static final Map<Type, Type> BOX_MAP = new HashMap<>(8);

  static {
    // We treat void and Void as nonnull
    UNBOX_MAP.put(Boolean.class,   boolean.class);
    UNBOX_MAP.put(Byte.class,      byte.class);
    UNBOX_MAP.put(Character.class, char.class);
    UNBOX_MAP.put(Short.class,     short.class);
    UNBOX_MAP.put(Integer.class,   int.class);
    UNBOX_MAP.put(Long.class,      long.class);
    UNBOX_MAP.put(Float.class,     float.class);
    UNBOX_MAP.put(Double.class,    double.class);

    BOX_MAP.put(boolean.class, Boolean.class);
    BOX_MAP.put(byte.class,    Byte.class);
    BOX_MAP.put(char.class,    Character.class);
    BOX_MAP.put(short.class,   Short.class);
    BOX_MAP.put(int.class,     Integer.class);
    BOX_MAP.put(long.class,    Long.class);
    BOX_MAP.put(float.class,   Float.class);
    BOX_MAP.put(double.class,  Double.class);
  }

  /**
   * Returns {@code true} if the type is non-null.
   */
  public static boolean isNonNull(Type type) {
    return type instanceof NonNullType || BOX_MAP.keySet().contains(type);
  }

  /**
   * Returns the nullable type of the type.
   */
  public static Type nullableOf(Type type) {
    if (type instanceof NonNullType) {
      return ((NonNullType) type).getNullableType();
    } else if (type instanceof Class) {
      Type boxedType = BOX_MAP.get(type);
      return boxedType != null ? boxedType : type;
    } else {
      return type;
    }
  }

  /**
   * Returns the non-null type of the type.
   */
  public static Type nonNullOf(Type type) {
    if (type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      if (clazz.isPrimitive()) {
        return clazz;
      } else {
        Type unboxedType = UNBOX_MAP.get(type);
        if (unboxedType != null) {
          return unboxedType;
        } else {
          return new NonNullTypeImpl(type);
        }
      }
    } else if (type instanceof NonNullType) {
      return type;
    } else {
      return new NonNullTypeImpl(type);
    }
  }

  /**
   * Returns a new parameterized type, applying {@code typeArguments} to {@code rawType}. Use this
   * method if {@code rawType} is not enclosed in another type.
   */
  public static ParameterizedType newParameterizedType(Type rawType, Type... typeArguments) {
    return new ParameterizedTypeImpl(null, rawType, typeArguments);
  }

  /**
   * Returns a new parameterized type, applying {@code typeArguments} to {@code rawType}. Use this
   * method if {@code rawType} is enclosed in {@code ownerType}.
   */
  public static ParameterizedType newParameterizedTypeWithOwner(
      Type ownerType, Type rawType, Type... typeArguments) {
    return new ParameterizedTypeImpl(ownerType, rawType, typeArguments);
  }

  /** Returns an array type whose elements are all instances of {@code componentType}. */
  public static GenericArrayType arrayOf(Type componentType) {
    return new GenericArrayTypeImpl(componentType);
  }

  /**
   * Returns a type that represents an unknown type that extends {@code bound}. For example, if
   * {@code bound} is {@code CharSequence.class}, this returns {@code ? extends CharSequence}. If
   * {@code bound} is {@code Object.class}, this returns {@code ?}, which is shorthand for {@code
   * ? extends Object}.
   */
  public static WildcardType subtypeOf(Type bound) {
    return new WildcardTypeImpl(new Type[] { bound }, EMPTY_TYPE_ARRAY);
  }

  /**
   * Returns a type that represents an unknown supertype of {@code bound}. For example, if {@code
   * bound} is {@code String.class}, this returns {@code ? super String}.
   */
  public static WildcardType supertypeOf(Type bound) {
    return new WildcardTypeImpl(new Type[] { Object.class }, new Type[] { bound });
  }

  /**
   * Returns the {@code Type} object representing the class or interface
   * that declared this type.
   */
  public static Class<?> getRawType(Type type) {
    if (type instanceof Class<?>) {
      // type is a normal class.
      return (Class<?>) type;

    } else if (type instanceof NonNullType) {
      Type nullableType = ((NonNullType) type).getNullableType();
      return getRawType(nullableType);

    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
      // suspects some pathological case related to nested classes exists.
      Type rawType = parameterizedType.getRawType();
      return (Class<?>) rawType;

    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();

    } else if (type instanceof TypeVariable) {
      // We could use the variable's bounds, but that won't work if there are multiple. having a raw
      // type that's more general than necessary is okay.
      return Object.class;

    } else if (type instanceof WildcardType) {
      return getRawType(((WildcardType) type).getUpperBounds()[0]);

    } else {
      String className = type == null ? "null" : type.getClass().getName();
      throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
          + "GenericArrayType, but <" + type + "> is of type " + className);
    }
  }

  /** Returns true if {@code a} and {@code b} are equal. */
  public static boolean equals(@Nullable Type a, @Nullable Type b) {
    if (a == b) {
      return true; // Also handles (a == null && b == null).

    } else if (a instanceof Class) {
      if (b instanceof GenericArrayType) {
        return equals(((Class) a).getComponentType(),
            ((GenericArrayType) b).getGenericComponentType());
      }
      return a.equals(b); // Class already specifies equals().

    } else if (a instanceof NonNullType) {
      if (b instanceof NonNullType) {
        return equals(((NonNullType) a).getNullableType(),
            ((NonNullType) b).getNullableType());
      }
      return false;

    } else if (a instanceof ParameterizedType) {
      if (!(b instanceof ParameterizedType)) return false;
      ParameterizedType pa = (ParameterizedType) a;
      ParameterizedType pb = (ParameterizedType) b;
      Type[] aTypeArguments = pa instanceof ParameterizedTypeImpl
          ? ((ParameterizedTypeImpl) pa).typeArguments
          : pa.getActualTypeArguments();
      Type[] bTypeArguments = pb instanceof ParameterizedTypeImpl
          ? ((ParameterizedTypeImpl) pb).typeArguments
          : pb.getActualTypeArguments();
      return equals(pa.getOwnerType(), pb.getOwnerType())
          && pa.getRawType().equals(pb.getRawType())
          && Arrays.equals(aTypeArguments, bTypeArguments);

    } else if (a instanceof GenericArrayType) {
      if (b instanceof Class) {
        return equals(((Class) b).getComponentType(),
            ((GenericArrayType) a).getGenericComponentType());
      }
      if (!(b instanceof GenericArrayType)) return false;
      GenericArrayType ga = (GenericArrayType) a;
      GenericArrayType gb = (GenericArrayType) b;
      return equals(ga.getGenericComponentType(), gb.getGenericComponentType());

    } else if (a instanceof WildcardType) {
      if (!(b instanceof WildcardType)) return false;
      WildcardType wa = (WildcardType) a;
      WildcardType wb = (WildcardType) b;
      return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
          && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

    } else if (a instanceof TypeVariable) {
      if (!(b instanceof TypeVariable)) return false;
      TypeVariable<?> va = (TypeVariable<?>) a;
      TypeVariable<?> vb = (TypeVariable<?>) b;
      return va.getGenericDeclaration() == vb.getGenericDeclaration()
          && va.getName().equals(vb.getName());

    } else {
      // This isn't a supported type.
      return false;
    }
  }

  private static Type getSupertype(Type context, Class<?> contextRawType, Class<?> supertype) {
    if (!supertype.isAssignableFrom(contextRawType)) throw new IllegalArgumentException();
    return resolve(context, contextRawType,
        getGenericSupertype(context, contextRawType, supertype));
  }

  /**
   * Returns the generic form of {@code supertype}, a superclass or interface.
   */
  public static Type getSupertype(Type type, Class<?> supertype) {
    boolean isNonNull = type instanceof NonNullType;
    Type result = getSupertype(type, getRawType(type), supertype);
    return isNonNull ? nonNullOf(result) : result;
  }

  /**
   * Returns the generic form of superclass.
   */
  public static Type getGenericSuperclass(Type type) {
    boolean isNonNull = type instanceof NonNullType;
    Class<?> rawType = Types.getRawType(type);
    Type result = resolve(type, rawType, rawType.getGenericSuperclass());
    return isNonNull ? nonNullOf(result) : result;
  }

  /**
   * Returns the generic form of interfaces.
   */
  public static Type[] getGenericInterfaces(Type type) {
    boolean isNonNull = type instanceof NonNullType;
    Class<?> rawType = Types.getRawType(type);
    Type[] interfaces = rawType.getGenericInterfaces();
    Type[] result = new Type[interfaces.length];
    for (int i = 0; i < interfaces.length; i++) {
      result[i] = resolve(type, rawType, interfaces[i]);
      result[i] = isNonNull ? nonNullOf(result[i]) : result[i];
    }
    return result;
  }

  /**
   * Returns the element type of {@code type} if it is an array type, or null if it is not an
   * array type.
   */
  public static Type arrayComponentType(Type type) {
    if (type instanceof NonNullType) {
      type = ((NonNullType) type).getNullableType();
    }
    if (type instanceof GenericArrayType) {
      return ((GenericArrayType) type).getGenericComponentType();
    } else if (type instanceof Class) {
      return ((Class<?>) type).getComponentType();
    } else {
      return null;
    }
  }

  /**
   * Returns a type that is functionally equal but not necessarily equal according to {@link
   * Object#equals(Object) Object.equals()}.
   */
  public static Type canonicalize(Type type) {
    if (type instanceof Class) {
      Class<?> c = (Class<?>) type;
      return c.isArray() ? new GenericArrayTypeImpl(canonicalize(c.getComponentType())) : c;

    } else if (type instanceof NonNullType) {
      if (type instanceof NonNullTypeImpl) return type;
      return new NonNullTypeImpl(((NonNullType) type).getNullableType());

    } else if (type instanceof ParameterizedType) {
      if (type instanceof ParameterizedTypeImpl) return type;
      ParameterizedType p = (ParameterizedType) type;
      return new ParameterizedTypeImpl(p.getOwnerType(),
          p.getRawType(), p.getActualTypeArguments());

    } else if (type instanceof GenericArrayType) {
      if (type instanceof GenericArrayTypeImpl) return type;
      GenericArrayType g = (GenericArrayType) type;
      return new GenericArrayTypeImpl(g.getGenericComponentType());

    } else if (type instanceof WildcardType) {
      if (type instanceof WildcardTypeImpl) return type;
      WildcardType w = (WildcardType) type;
      return new WildcardTypeImpl(w.getUpperBounds(), w.getLowerBounds());

    } else {
      return type; // This type is unsupported!
    }
  }

  /**
   * If type is a "? extends X" wildcard, returns X; otherwise returns type unchanged.
   */
  public static Type removeSubtypeWildcard(Type type) {
    boolean isNonNull = false;
    Type original = type;
    if (type instanceof NonNullType) {
      isNonNull = true;
      type = ((NonNullType) type).getNullableType();
    }

    if (!(type instanceof WildcardType)) return original;

    Type[] lowerBounds = ((WildcardType) type).getLowerBounds();
    if (lowerBounds.length != 0) return original;

    Type[] upperBounds = ((WildcardType) type).getUpperBounds();
    if (upperBounds.length != 1) throw new IllegalArgumentException();

    Type result = upperBounds[0];
    return isNonNull ? nonNullOf(result) : result;
  }

  public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
    // This implementation is made a little more complicated in an attempt to avoid object-creation.
    boolean isNonNull = false;
    Type original = toResolve;
    Type firstToResolve = toResolve;
    if (toResolve instanceof NonNullType) {
      isNonNull = true;
      toResolve = ((NonNullType) toResolve).getNullableType();
      firstToResolve = toResolve;
    }

    while (true) {
      if (toResolve instanceof TypeVariable) {
        TypeVariable<?> thisOriginal = (TypeVariable<?>) toResolve;
        toResolve = resolveTypeVariable(context, contextRawType, thisOriginal);
        if (toResolve == firstToResolve) return original;
        if (toResolve == thisOriginal) return isNonNull ? nonNullOf(toResolve) : toResolve;

      } else if (toResolve instanceof Class && ((Class<?>) toResolve).isArray()) {
        Class<?> thisOriginal = (Class<?>) toResolve;
        Type componentType = thisOriginal.getComponentType();
        Type newComponentType = resolve(context, contextRawType, componentType);
        if (componentType == newComponentType) {
          if (toResolve == firstToResolve) return original;
          return isNonNull ? nonNullOf(toResolve) : toResolve;
        } else {
          toResolve = arrayOf(newComponentType);
          return isNonNull ? nonNullOf(toResolve) : toResolve;
        }

      } else if (toResolve instanceof GenericArrayType) {
        GenericArrayType thisOriginal = (GenericArrayType) toResolve;
        Type componentType = thisOriginal.getGenericComponentType();
        Type newComponentType = resolve(context, contextRawType, componentType);
        if (componentType == newComponentType) {
          if (toResolve == firstToResolve) return original;
          return isNonNull ? nonNullOf(toResolve) : toResolve;
        } else {
          toResolve = arrayOf(newComponentType);
          return isNonNull ? nonNullOf(toResolve) : toResolve;
        }

      } else if (toResolve instanceof ParameterizedType) {
        ParameterizedType thisOriginal = (ParameterizedType) toResolve;
        Type ownerType = thisOriginal.getOwnerType();
        Type newOwnerType = resolve(context, contextRawType, ownerType);
        boolean changed = newOwnerType != ownerType;

        Type[] args = thisOriginal.getActualTypeArguments();
        for (int t = 0, length = args.length; t < length; t++) {
          Type resolvedTypeArgument = resolve(context, contextRawType, args[t]);
          if (resolvedTypeArgument != args[t]) {
            if (!changed) {
              args = args.clone();
              changed = true;
            }
            args[t] = resolvedTypeArgument;
          }
        }

        if (changed) {
          toResolve = new ParameterizedTypeImpl(newOwnerType, thisOriginal.getRawType(), args);
          return isNonNull ? nonNullOf(toResolve) : toResolve;
        } else {
          if (toResolve == firstToResolve) return original;
          return isNonNull ? nonNullOf(toResolve) : toResolve;
        }

      } else if (toResolve instanceof WildcardType) {
        WildcardType thisOriginal = (WildcardType) toResolve;
        Type[] originalLowerBound = thisOriginal.getLowerBounds();
        Type[] originalUpperBound = thisOriginal.getUpperBounds();

        if (originalLowerBound.length == 1) {
          Type lowerBound = resolve(context, contextRawType, originalLowerBound[0]);
          if (lowerBound != originalLowerBound[0]) {
            toResolve = supertypeOf(lowerBound);
            return isNonNull ? nonNullOf(toResolve) : toResolve;
          }
        } else if (originalUpperBound.length == 1) {
          Type upperBound = resolve(context, contextRawType, originalUpperBound[0]);
          if (upperBound != originalUpperBound[0]) {
            toResolve = subtypeOf(upperBound);
            return isNonNull ? nonNullOf(toResolve) : toResolve;
          }
        }
        if (toResolve == firstToResolve) return original;
        return isNonNull ? nonNullOf(toResolve) : toResolve;

      } else {
        return toResolve;
      }
    }
  }

  private static Type resolveTypeVariable(Type context, Class<?> contextRawType, TypeVariable<?> unknown) {
    Class<?> declaredByRaw = declaringClassOf(unknown);

    // We can't reduce this further.
    if (declaredByRaw == null) return unknown;

    Type declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw);
    if (declaredBy instanceof NonNullType) {
      declaredBy = ((NonNullType) declaredBy).getNullableType();
    }
    if (declaredBy instanceof ParameterizedType) {
      int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
      return ((ParameterizedType) declaredBy).getActualTypeArguments()[index];
    }

    return unknown;
  }

  /**
   * Returns the generic supertype for {@code supertype}. For example, given a class {@code
   * IntegerSet}, the result for when supertype is {@code Set.class} is {@code Set<Integer>} and the
   * result when the supertype is {@code Collection.class} is {@code Collection<Integer>}.
   */
  private static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
    if (toResolve == rawType) {
      return context;
    }

    // we skip searching through interfaces if unknown is an interface
    if (toResolve.isInterface()) {
      Class<?>[] interfaces = rawType.getInterfaces();
      for (int i = 0, length = interfaces.length; i < length; i++) {
        if (interfaces[i] == toResolve) {
          return rawType.getGenericInterfaces()[i];
        } else if (toResolve.isAssignableFrom(interfaces[i])) {
          return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
        }
      }
    }

    // check our supertypes
    if (!rawType.isInterface()) {
      while (rawType != Object.class) {
        Class<?> rawSupertype = rawType.getSuperclass();
        if (rawSupertype == toResolve) {
          return rawType.getGenericSuperclass();
        } else if (toResolve.isAssignableFrom(rawSupertype)) {
          return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
        }
        rawType = rawSupertype;
      }
    }

    // we can't resolve this further
    return toResolve;
  }

  private static int hashCodeOrZero(@Nullable Object o) {
    return o != null ? o.hashCode() : 0;
  }

  private static String typeToString(Type type) {
    return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
  }

  private static int indexOf(Object[] array, Object toFind) {
    for (int i = 0; i < array.length; i++) {
      if (toFind.equals(array[i])) return i;
    }
    throw new NoSuchElementException();
  }

  /**
   * Returns the declaring class of {@code typeVariable}, or {@code null} if it was not declared by
   * a class.
   */
  @Nullable
  private static Class<?> declaringClassOf(TypeVariable<?> typeVariable) {
    GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
    return genericDeclaration instanceof Class ? (Class<?>) genericDeclaration : null;
  }

  private static void checkNotPrimitive(Type type) {
    if ((type instanceof Class<?>) && ((Class<?>) type).isPrimitive()) {
      throw new IllegalArgumentException("Unexpected primitive " + type + ". Use the boxed type.");
    }
  }

  private static final class NonNullTypeImpl implements NonNullType {
    private final Type nullableType;

    NonNullTypeImpl(Type nullableType) {
      this.nullableType = canonicalize(nullableType);
    }

    @Override
    public Type getNullableType() {
      return nullableType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof NonNullType
          && Types.equals(this, (NonNullType) obj);
    }

    @Override
    public int hashCode() {
      return nullableType.hashCode() * 31 + 1;
    }

    @NonNull
    @Override
    public String toString() {
      return nullableType.toString() + "!";
    }
  }

  private static final class ParameterizedTypeImpl implements ParameterizedType {
    @Nullable
    private final Type ownerType;
    private final Type rawType;
    final Type[] typeArguments;

    ParameterizedTypeImpl(@Nullable Type ownerType, Type rawType, Type... typeArguments) {
      // Require an owner type if the raw type needs it.
      if (rawType instanceof Class<?>) {
        Class<?> enclosingClass = ((Class<?>) rawType).getEnclosingClass();
        if (ownerType != null) {
          if (enclosingClass == null || Types.getRawType(ownerType) != enclosingClass) {
            throw new IllegalArgumentException(
                "unexpected owner type for " + rawType + ": " + ownerType);
          }
        } else if (enclosingClass != null) {
          throw new IllegalArgumentException(
              "unexpected owner type for " + rawType + ": null");
        }
      }

      this.ownerType = ownerType == null ? null : canonicalize(ownerType);
      this.rawType = canonicalize(rawType);
      if (!(this.rawType instanceof Class)) throw new IllegalArgumentException("Raw type must be a Class, but it's a " + rawType.getClass().getName());
      this.typeArguments = typeArguments.clone();
      for (int t = 0; t < this.typeArguments.length; t++) {
        if (this.typeArguments[t] == null) throw new NullPointerException();
        checkNotPrimitive(this.typeArguments[t]);
        this.typeArguments[t] = canonicalize(this.typeArguments[t]);
      }
    }

    @Override
    public Type[] getActualTypeArguments() {
      return typeArguments.clone();
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public @Nullable Type getOwnerType() {
      return ownerType;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ParameterizedType
          && Types.equals(this, (ParameterizedType) other);
    }

    @Override
    public int hashCode() {
      return 31 * (31 * hashCodeOrZero(rawType) + hashCodeOrZero(ownerType)) +
          Arrays.hashCode(typeArguments);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder(30 * (typeArguments.length + 1));
      result.append(typeToString(rawType));

      if (typeArguments.length == 0) {
        return result.toString();
      }

      result.append("<").append(typeToString(typeArguments[0]));
      for (int i = 1; i < typeArguments.length; i++) {
        result.append(", ").append(typeToString(typeArguments[i]));
      }
      return result.append(">").toString();
    }
  }

  private static final class GenericArrayTypeImpl implements GenericArrayType {
    private final Type componentType;

    GenericArrayTypeImpl(Type componentType) {
      this.componentType = canonicalize(componentType);
    }

    @Override
    public Type getGenericComponentType() {
      return componentType;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof GenericArrayType
          && Types.equals(this, (GenericArrayType) o);
    }

    @Override
    public int hashCode() {
      return componentType.hashCode();
    }

    @Override
    public String toString() {
      return typeToString(componentType) + "[]";
    }
  }

  /**
   * The WildcardType interface supports multiple upper bounds and multiple lower bounds. We only
   * support what the Java 6 language needs - at most one bound. If a lower bound is set, the upper
   * bound must be Object.class.
   */
  private static final class WildcardTypeImpl implements WildcardType {
    private final Type upperBound;
    @Nullable
    private final Type lowerBound;

    WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
      if (lowerBounds.length > 1) throw new IllegalArgumentException();
      if (upperBounds.length != 1) throw new IllegalArgumentException();

      if (lowerBounds.length == 1) {
        if (lowerBounds[0] == null) throw new NullPointerException();
        checkNotPrimitive(lowerBounds[0]);
        if (upperBounds[0] != Object.class) throw new IllegalArgumentException();
        this.lowerBound = canonicalize(lowerBounds[0]);
        this.upperBound = Object.class;

      } else {
        if (upperBounds[0] == null) throw new NullPointerException();
        checkNotPrimitive(upperBounds[0]);
        this.lowerBound = null;
        this.upperBound = canonicalize(upperBounds[0]);
      }
    }

    @Override
    public Type[] getUpperBounds() {
      return new Type[] { upperBound };
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBound != null ? new Type[] { lowerBound } : EMPTY_TYPE_ARRAY;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof WildcardType
          && Types.equals(this, (WildcardType) other);
    }

    @Override
    public int hashCode() {
      // This equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds()).
      return (lowerBound != null ? 31 + lowerBound.hashCode() : 1)
          ^ (31 + upperBound.hashCode());
    }

    @Override
    public String toString() {
      if (lowerBound != null) {
        return "? super " + typeToString(lowerBound);
      } else if (upperBound == Object.class) {
        return "?";
      } else {
        return "? extends " + typeToString(upperBound);
      }
    }
  }
}
