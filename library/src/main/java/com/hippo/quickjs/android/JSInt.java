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

final class JSInt extends JSNumber {

  private volatile boolean cached = false;
  private volatile int cache;

  JSInt(long pointer, JSContext jsContext) {
    super(pointer, jsContext);
  }

  private int getIntInRange(String javaType, int min, int max) {
    int value = getInt();
    if (min <= value && value <= max) {
      return value;
    } else {
      throw new JSDataException("Can't treat " + value + " as " + javaType);
    }
  }

  @Override
  public byte getByte() {
    return (byte) getIntInRange("byte", Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  @Override
  public short getShort() {
    return (short) getIntInRange("short", Short.MIN_VALUE, Short.MAX_VALUE);
  }

  @Override
  public int getInt() {
    if (!cached) {
      synchronized (jsContext.jsRuntime) {
        if (!cached) {
          jsContext.checkClosed();
          cache = QuickJS.getValueInt(pointer);
          cached = true;
        }
      }
    }
    return cache;
  }

  @Override
  public long getLong() {
    return getInt();
  }

  @Override
  public float getFloat() {
    return getInt();
  }

  @Override
  public double getDouble() {
    return getInt();
  }
}
