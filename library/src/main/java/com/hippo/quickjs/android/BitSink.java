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

import java.nio.charset.Charset;

/**
 * BitSink pickles java value.
 */
public class BitSink {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private static final int DEFAULT_LENGTH = 16;

  private byte[] bytes;
  private int offset;

  public BitSink() {
    bytes = new byte[DEFAULT_LENGTH];
    offset = 0;
  }

  private void ensureSize(int size) {
    int newSize = offset + size;
    if (newSize > bytes.length) {
      // TODO check overflow
      newSize = newSize << 1;
      byte[] newBuffer = new byte[newSize];
      System.arraycopy(bytes, 0, newBuffer, 0, offset);
      bytes = newBuffer;
    }
  }

  /**
   * Writes a boolean value.
   */
  public void writeBoolean(boolean value) {
    final int size = 1;
    ensureSize(size);
    bytes[offset] = value ? (byte) 1 : (byte) 0;
    offset += size;
  }

  /**
   * Writes a int value.
   */
  public void writeInt(int value) {
    final int size = 4;
    ensureSize(size);
    Bits.writeInt(bytes, offset, value);
    offset += size;
  }

  /**
   * Writes a long value.
   */
  public void writeLong(long value) {
    final int size = 8;
    ensureSize(size);
    Bits.writeLong(bytes, offset, value);
    offset += size;
  }

  /**
   * Writes a double value.
   */
  public void writeDouble(double value) {
    final int size = 8;
    ensureSize(size);
    Bits.writeLong(bytes, offset, Double.doubleToRawLongBits(value));
    offset += size;
  }

  /**
   * Writes a string value.
   */
  public void writeString(String value) {
    byte[] strBytes = value.getBytes(UTF_8);
    final int length = strBytes.length;
    final int size = length + 1;
    writeInt(size);

    ensureSize(size);
    System.arraycopy(strBytes, 0, this.bytes, offset, length);
    bytes[offset + length] = 0;
    offset += size;
  }

  byte[] getBytes() {
    return bytes;
  }

  int getSize() {
    return offset;
  }
}
