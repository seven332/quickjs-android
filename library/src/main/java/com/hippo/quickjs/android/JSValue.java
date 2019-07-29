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

  public static final int VALUE_TAG_UNKNOWN = Integer.MIN_VALUE;
  public static final int VALUE_TAG_SYMBOL = -8;
  public static final int VALUE_TAG_STRING = -7;
  public static final int VALUE_TAG_OBJECT = -1;
  public static final int VALUE_TAG_INT = 0;
  public static final int VALUE_TAG_BOOL = 1;
  public static final int VALUE_TAG_NULL = 2;
  public static final int VALUE_TAG_UNDEFINED = 3;
  public static final int VALUE_TAG_EXCEPTION = 6;
  public static final int VALUE_TAG_DOUBLE = 7;

  private final long pointer;
  private final JSContext jsContext;

  JSValue(long pointer, JSContext jsContext) {
    this.pointer = pointer;
    this.jsContext = jsContext;
  }

  public int getTag() {
    return jsContext.getValueTag(pointer);
  }

  public boolean getBoolean() {
    return jsContext.getValueBoolean(pointer);
  }

  public int getInt() {
    if (getTag() == VALUE_TAG_DOUBLE) {
      double value = jsContext.getValueDouble(pointer);
      int iPart = (int) value;
      double fPart = value - iPart;
      if (fPart == 0.0f) {
        return iPart;
      } else {
        throw new JSDataException("Can treat number with decimal part as int: " + value);
      }
    } else {
      return jsContext.getValueInt(pointer);
    }
  }

  public double getDouble() {
    if (getTag() == VALUE_TAG_INT) {
      return jsContext.getValueInt(pointer);
    } else {
      return jsContext.getValueDouble(pointer);
    }
  }

  public String getString() {
    return jsContext.getValueString(pointer);
  }
}
