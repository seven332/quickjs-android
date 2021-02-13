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

import org.junit.Test;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.junit.Assert.*;

public class JSArrayBufferTest extends TestsWithContext {

  @Test
  public void toBooleanArray() {
    boolean[] booleans = new boolean[] {true, false, true, true, false};
    JSArrayBuffer buffer = context.createJSArrayBuffer(booleans);
    assertArrayEquals(booleans, buffer.toBooleanArray());
  }

  @Test
  public void toByteArray() {
    byte[] bytes = new byte[] {1, 2, 3, 4, 5};
    JSArrayBuffer buffer = context.createJSArrayBuffer(bytes);
    assertArrayEquals(bytes, buffer.toByteArray());
  }

  @Test
  public void toCharArray() {
    char[] chars = new char[] {'1', '2', '3', '4', '5'};
    JSArrayBuffer buffer = context.createJSArrayBuffer(chars);
    assertArrayEquals(chars, buffer.toCharArray());
  }

  @Test
  public void toShortArray() {
    short[] shorts = new short[] {10, 20, 30, 40, 50};
    JSArrayBuffer buffer = context.createJSArrayBuffer(shorts);
    assertArrayEquals(shorts, buffer.toShortArray());
  }

  @Test
  public void toIntArray() {
    int[] ints = new int[] {10, 20, 30, 40, 50};
    JSArrayBuffer buffer = context.createJSArrayBuffer(ints);
    assertArrayEquals(ints, buffer.toIntArray());
  }

  @Test
  public void toLongArray() {
    long[] longs = new long[] {100, 200, 300, 400, 500};
    JSArrayBuffer buffer = context.createJSArrayBuffer(longs);
    assertArrayEquals(longs, buffer.toLongArray());
  }

  @Test
  public void toFloatArray() {
    float[] floats = new float[] {1.1f, 2.2f, 3.3f, 4.4f, 5.5f};
    JSArrayBuffer buffer = context.createJSArrayBuffer(floats);
    assertArrayEquals(floats, buffer.toFloatArray(), 0);
  }

  @Test
  public void toDoubleArray() {
    double[] doubles = new double[] {1.11, 2.22, 3.33, 4.44, 5.55};
    JSArrayBuffer buffer = context.createJSArrayBuffer(doubles);
    assertArrayEquals(doubles, buffer.toDoubleArray(), 0);
  }

  @Test
  public void toLongArray_mismatchedSize_exception() {
    byte[] bytes = new byte[] {1, 2, 3, 4, 5};
    JSArrayBuffer buffer = context.createJSArrayBuffer(bytes);
    assertException(
      IllegalStateException.class,
      "Size not matched",
      buffer::toLongArray
    );
  }
}
