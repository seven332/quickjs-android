/*
 * Copyright 2021 Hippo Seven
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

public class JSArrayBuffer extends JSObject {

  JSArrayBuffer(long pointer, JSContext jsContext) {
    super(pointer, jsContext, null);
  }

  public int getByteLength() {
    return getProperty("byteLength").cast(JSNumber.class).getInt();
  }

  public boolean[] toBooleanArray() {
    return QuickJS.toBooleanArray(jsContext.pointer, pointer);
  }

  public byte[] toByteArray() {
    return QuickJS.toByteArray(jsContext.pointer, pointer);
  }

  /**
   * @throws IllegalStateException if its byteLength isn't a multiple of 2
   */
  public char[] toCharArray() {
    return QuickJS.toCharArray(jsContext.pointer, pointer);
  }

  /**
   * @throws IllegalStateException if its byteLength isn't a multiple of 2
   */
  public short[] toShortArray() {
    return QuickJS.toShortArray(jsContext.pointer, pointer);
  }

  /**
   * @throws IllegalStateException if its byteLength isn't a multiple of 4
   */
  public int[] toIntArray() {
    return QuickJS.toIntArray(jsContext.pointer, pointer);
  }

  /**
   * @throws IllegalStateException if its byteLength isn't a multiple of 8
   */
  public long[] toLongArray() {
    return QuickJS.toLongArray(jsContext.pointer, pointer);
  }

  /**
   * @throws IllegalStateException if its byteLength isn't a multiple of 4
   */
  public float[] toFloatArray() {
    return QuickJS.toFloatArray(jsContext.pointer, pointer);
  }

  /**
   * @throws IllegalStateException if its byteLength isn't a multiple of 8
   */
  public double[] toDoubleArray() {
    return QuickJS.toDoubleArray(jsContext.pointer, pointer);
  }
}
