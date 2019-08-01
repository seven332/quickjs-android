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

// TODO Make JSValue closeable?

/**
 * JSValue is a Javascript value.
 * It could be a number, a object, null, undefined or something else.
 */
public final class JSValue {

  public static final int TYPE_SYMBOL = -8;
  public static final int TYPE_STRING = -7;
  public static final int TYPE_OBJECT = -1;
  public static final int TYPE_INT = 0;
  public static final int TYPE_BOOLEAN = 1;
  public static final int TYPE_NULL = 2;
  public static final int TYPE_UNDEFINED = 3;
  public static final int TYPE_EXCEPTION = 6;
  public static final int TYPE_DOUBLE = 7;

  private final long pointer;
  private final JSContext jsContext;

  JSValue(long pointer, JSContext jsContext) {
    this.pointer = pointer;
    this.jsContext = jsContext;
  }

  /**
   * Returns the raw type in QuickJS c code.
   */
  public int getType() {
    return jsContext.getValueTag(pointer);
  }

  /**
   * Returns true if the JSValue is an array.
   */
  public boolean isArray() {
    return jsContext.isValueArray(pointer);
  }

  /**
   * Returns the property as a JSValue.
   * The type of the JSValue is {@link #TYPE_UNDEFINED} if the property doesn't exist.
   *
   * @throws JSEvaluationException if the cannot read property of this JSValue.
   */
  public JSValue getProperty(int index) {
    return jsContext.getValueProperty(pointer, index);
  }

  /**
   * Returns the property as a JSValue.
   * The type of the JSValue is {@link #TYPE_UNDEFINED} if the property doesn't exist.
   *
   * @throws JSEvaluationException if the cannot read property of this JSValue.
   */
  public JSValue getProperty(String name) {
    return jsContext.getValueProperty(pointer, name);
  }

  private String wrongTypeMessage(String javaType, int jsType) {
    return "Can't treat the JSValue as " + javaType + ", it's type is " + jsType;
  }

  private String wrongNumberMessage(String javaType, int number) {
    return "Can't treat the number as " + javaType + ": " + number;
  }

  private String wrongNumberMessage(String javaType, double number) {
    return "Can't treat the number as " + javaType + ": " + number;
  }

  /**
   * Gets boolean value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_BOOLEAN}
   */
  public boolean getBoolean() {
    int type = getType();
    switch (type) {
      case TYPE_BOOLEAN:
        return jsContext.getValueBoolean(pointer);
      default:
        throw new JSDataException(wrongTypeMessage("boolean", type));
    }
  }

  private int getInt(String javaType) {
    int type = getType();
    switch (type) {
      case TYPE_INT:
        return jsContext.getValueInt(pointer);
      case TYPE_DOUBLE:
        double value = jsContext.getValueDouble(pointer);
        int iPart = (int) value;
        double fPart = value - iPart;
        if (fPart == 0.0f) {
          return iPart;
        } else {
          throw new JSDataException(wrongNumberMessage(javaType, value));
        }
      default:
        throw new JSDataException(wrongTypeMessage(javaType, type));
    }
  }

  private int getIntInRange(String javaType, int min, int max) {
    int value = getInt(javaType);
    if (min <= value && value <= max) {
      return value;
    } else {
      throw new JSDataException(wrongNumberMessage(javaType, value));
    }
  }

  /**
   * Gets byte value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_INT} and not {@link #TYPE_DOUBLE},
   *         or has decimal part, or bigger than {@link Byte#MAX_VALUE} or smaller than {@link Byte#MIN_VALUE}
   */
  public byte getByte() {
    return (byte) getIntInRange("byte", Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  /**
   * Gets char value of this JSValue. Only works if the JSValue is {@link #TYPE_STRING}
   * and string length is {@code 1}.
   *
   * @throws JSDataException if it's not {@link #TYPE_STRING},
   *         or string length is not {@code 1}
   */
  public char getChar() {
    int type = getType();
    switch (type) {
      case TYPE_STRING:
        String str = jsContext.getValueString(pointer);
        if (str.length() != 1) {
          throw new JSDataException("Can't treat the string as a char: \"" + str + "\"");
        }
        return str.charAt(0);
      default:
        throw new JSDataException(wrongTypeMessage("char", type));
    }
  }

  /**
   * Get short value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_INT} and not {@link #TYPE_DOUBLE},
   *         or has decimal part, or bigger than {@link Short#MAX_VALUE} or smaller than {@link Short#MIN_VALUE}
   */
  public short getShort() {
    return (short) getIntInRange("short", Short.MIN_VALUE, Short.MAX_VALUE);
  }

  /**
   * Get int value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_INT} and not {@link #TYPE_DOUBLE},
   *         or has decimal part, or bigger than {@link Integer#MAX_VALUE} or smaller than {@link Integer#MIN_VALUE}
   */
  public int getInt() {
    return getInt("int");
  }

  /**
   * Get int value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_INT} and not {@link #TYPE_DOUBLE},
   *         or has decimal part, or bigger than {@link Long#MAX_VALUE} or smaller than {@link Long#MIN_VALUE}
   */
  public long getLong() {
    int type = getType();
    switch (type) {
      case TYPE_INT:
        return jsContext.getValueInt(pointer);
      case TYPE_DOUBLE:
        double value = jsContext.getValueDouble(pointer);
        long iPart = (long) value;
        double fPart = value - iPart;
        if (fPart == 0.0f) {
          return iPart;
        } else {
          throw new JSDataException(wrongNumberMessage("long", value));
        }
      default:
        throw new JSDataException(wrongTypeMessage("long", type));
    }
  }

  /**
   * Gets float value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_INT} and not {@link #TYPE_DOUBLE}
   */
  public float getFloat() {
    int type = getType();
    switch (type) {
      case TYPE_INT:
        return jsContext.getValueInt(pointer);
      case TYPE_DOUBLE:
        return (float) jsContext.getValueDouble(pointer);
      default:
        throw new JSDataException(wrongTypeMessage("float", type));
    }
  }

  /**
   * Gets double value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_INT} and not {@link #TYPE_DOUBLE}
   */
  public double getDouble() {
    int type = getType();
    switch (type) {
      case TYPE_INT:
        return jsContext.getValueInt(pointer);
      case TYPE_DOUBLE:
        return jsContext.getValueDouble(pointer);
      default:
        throw new JSDataException(wrongTypeMessage("double", type));
    }
  }

  /**
   * Gets string value of this JSValue.
   *
   * @throws JSDataException if it's not {@link #TYPE_STRING}
   */
  public String getString() {
    int type = getType();
    switch (type) {
      case TYPE_STRING:
        return jsContext.getValueString(pointer);
      default:
        throw new JSDataException(wrongTypeMessage("string", type));
    }
  }
}
