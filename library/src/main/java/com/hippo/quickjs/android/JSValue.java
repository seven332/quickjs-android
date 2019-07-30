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

  public int getType() {
    return jsContext.getValueTag(pointer);
  }

  private String wrongTypeMessage(String javaType, int jsType) {
    return "Can't treat the JSValue as " + javaType + ", it's type is " + jsType;
  }

  public boolean getBoolean() {
    int type = getType();
    switch (type) {
      case TYPE_BOOLEAN:
        return jsContext.getValueBoolean(pointer);
      default:
        throw new JSDataException(wrongTypeMessage("boolean", type));
    }
  }

  public int getInt() {
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
          throw new JSDataException("Can't treat the number as int: " + value);
        }
      default:
        throw new JSDataException(wrongTypeMessage("int", type));
    }
  }

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
