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

final class JSFloat64 extends JSNumber {

  private volatile boolean cached = false;
  private volatile double cache;

  JSFloat64(long pointer, JSContext jsContext) {
    super(pointer, jsContext);
  }

  private String wrongNumberMessage(String javaType, double value) {
    return "Can't treat " + value + " as " + javaType;
  }

  @Override
  public byte getByte() {
    double value = getDouble();
    byte result = (byte) value;
    if (result != value) {
      throw new JSDataException(wrongNumberMessage("byte", value));
    }
    return result;
  }

  @Override
  public short getShort() {
    double value = getDouble();
    short result = (short) value;
    if (result != value) {
      throw new JSDataException(wrongNumberMessage("short", value));
    }
    return result;
  }

  @Override
  public int getInt() {
    double value = getDouble();
    int result = (int) value;
    if (result != value) {
      throw new JSDataException(wrongNumberMessage("int", value));
    }
    return result;
  }

  @Override
  public long getLong() {
    double value = getDouble();
    long result = (long) value;
    if (result != value) {
      throw new JSDataException(wrongNumberMessage("long", value));
    }
    return result;
  }

  @Override
  public float getFloat() {
    return (float) getDouble();
  }

  @Override
  public double getDouble() {
    if (!cached) {
      synchronized (jsContext.jsRuntime) {
        if (!cached) {
          jsContext.checkClosed();
          cache = QuickJS.getValueDouble(pointer);
          cached = true;
        }
      }
    }
    return cache;
  }
}
