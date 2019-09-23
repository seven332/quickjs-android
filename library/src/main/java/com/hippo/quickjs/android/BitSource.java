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

public class BitSource {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private static final byte TYPE_INT    = 0;
  private static final byte TYPE_DOUBLE = 1;

  private final byte[] bytes;
  private int offset = 0;

  public BitSource(byte[] bytes) {
    this.bytes = bytes;
  }

  private byte nextByte() {
    byte result = Bits.readByte(bytes, offset);
    offset += 1;
    return result;
  }

  private int nextInt() {
    int result = Bits.readInt(bytes, offset);
    offset += 4;
    return result;
  }

  private long nextLong() {
    long result = Bits.readLong(bytes, offset);
    offset += 8;
    return result;
  }

  private double nextDouble() {
    double result = Bits.readDouble(bytes, offset);
    offset += 8;
    return result;
  }

  private String nextString() {
    final int size = nextInt();
    String result = new String(bytes, offset, size, UTF_8);
    offset += size;
    return result;
  }

  private JSDataException unknownNumberType(byte type) {
    return new JSDataException("Unknown number type: " + type);
  }

  private JSDataException wrongNumber(String javaType, int value) {
    return new JSDataException("Can't treat " + value + " as " + javaType);
  }

  private JSDataException wrongNumber(String javaType, double value) {
    return new JSDataException("Can't treat " + value + " as " + javaType);
  }

  private JSDataException wrongString(String javaType, String value) {
    return new JSDataException("Can't treat \"" + value + "\" as " + javaType);
  }

  /**
   * Reads a boolean value.
   */
  public boolean readBoolean() {
    return nextByte() != 0;
  }

  private int readInt(String javaType) {
    byte type = nextByte();
    switch (type) {
      case TYPE_INT:
        return nextInt();
      case TYPE_DOUBLE:
        double value = nextDouble();
        int result = (int) value;
        if (result != value) throw wrongNumber(javaType, value);
        return result;
      default:
        throw unknownNumberType(type);
    }
  }

  private int readIntInRange(String javaType, int min, int max) {
    int value = readInt(javaType);
    if (min <= value && value <= max) return value;
    else throw wrongNumber(javaType, value);
  }

  /**
   * Reads a byte value.
   */
  public byte readByte() {
    return (byte) readIntInRange("byte", Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  /**
   * Reads a char value.
   */
  public char readChar() {
    String str = nextString();
    if (str.length() != 1) {
      throw wrongString("char", str);
    }
    return str.charAt(0);
  }

  /**
   * Reads a byte value.
   */
  public short readShort() {
    return (short) readIntInRange("short", Short.MIN_VALUE, Short.MAX_VALUE);
  }

  /**
   * Reads a int value.
   */
  public int readInt() {
    return readInt("int");
  }

  /**
   * Reads a long value.
   */
  public long readLong() {
    byte type = nextByte();
    switch (type) {
      case TYPE_INT:
        return nextInt();
      case TYPE_DOUBLE:
        double value = nextDouble();
        long result = (long) value;
        if (result != value) throw wrongNumber("long", value);
        return result;
      default:
        throw unknownNumberType(type);
    }
  }

  /**
   * Reads a float value.
   */
  public float readFloat() {
    byte type = nextByte();
    switch (type) {
      case TYPE_INT:
        return nextInt();
      case TYPE_DOUBLE:
        return (float) nextDouble();
      default:
        throw unknownNumberType(type);
    }
  }

  /**
   * Reads a double value.
   */
  public double readDouble() {
    byte type = nextByte();
    switch (type) {
      case TYPE_INT:
        return nextInt();
      case TYPE_DOUBLE:
        return nextDouble();
      default:
        throw unknownNumberType(type);
    }
  }

  /**
   * Reads a string value.
   */
  public String readString() {
    return nextString();
  }

  /**
   * Reads array length.
   */
  public int readArrayLength() {
    return nextInt();
  }

  /**
   * Reads ptr.
   */
  public long readPtr() {
    return nextLong();
  }

  void checkEOF() {
    if (offset != bytes.length) {
      throw new RuntimeException("BitSource not EOF");
    }
  }
}
