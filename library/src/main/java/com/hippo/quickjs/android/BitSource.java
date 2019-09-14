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

  private static final byte TYPE_NULL    = 0;
  private static final byte TYPE_BOOLEAN = 1;
  private static final byte TYPE_INT     = 2;
  private static final byte TYPE_DOUBLE  = 3;
  private static final byte TYPE_STRING  = 4;

  private final byte[] buffer;
  private int offset = 0;

  public BitSource(byte[] buffer) {
    this.buffer = buffer;
  }

  private void checkSize(int size) {
    if (offset + size > buffer.length) {
      throw new IllegalStateException("No more " + size + " byte(s)");
    }
  }

  private byte readByte() {
    final int size = 1;
    checkSize(size);
    byte result = buffer[offset];
    offset += size;
    return result;
  }

  private int readInt() {
    final int size = 4;
    checkSize(size);
    int result = (((buffer[offset + 3]       ) << 24) |
                  ((buffer[offset + 2] & 0xff) << 16) |
                  ((buffer[offset + 1] & 0xff) <<  8) |
                  ((buffer[offset    ] & 0xff)      ));
    offset += size;
    return result;
  }

  private double readDouble() {
    final int size = 8;
    checkSize(size);
    long result = ((((long) buffer[offset + 7]       ) << 56) |
                   (((long) buffer[offset + 6] & 0xff) << 48) |
                   (((long) buffer[offset + 5] & 0xff) << 40) |
                   (((long) buffer[offset + 4] & 0xff) << 32) |
                   (((long) buffer[offset + 3] & 0xff) << 24) |
                   (((long) buffer[offset + 2] & 0xff) << 16) |
                   (((long) buffer[offset + 1] & 0xff) <<  8) |
                   (((long) buffer[offset    ] & 0xff)      ));
    offset += size;
    return Double.longBitsToDouble(result);
  }

  private String readString() {
    final int size = readInt();
    checkSize(size);
    String result = new String(buffer, offset, size, UTF_8);
    offset += size;
    return result;
  }

  private JSDataException wrongTypeException(String javaType, byte type) {
    return new JSDataException("Can't treat type " + type + " as " + javaType);
  }

  private JSDataException wrongNumberException(String javaType, int value) {
    return new JSDataException("Can't treat " + value + " as " + javaType);
  }

  private JSDataException wrongNumberException(String javaType, double value) {
    return new JSDataException("Can't treat " + value + " as " + javaType);
  }

  private JSDataException wrongStringException(String javaType, String value) {
    return new JSDataException("Can't treat \"" + value + "\" as " + javaType);
  }

  public boolean nextIfNull() {
    checkSize(1);
    byte type = buffer[offset];
    if (type == TYPE_NULL) {
      offset += 1;
      return true;
    } else {
      return false;
    }
  }

  public void nextNull() {
    byte type = readByte();
    if (type != TYPE_NULL) {
      throw wrongTypeException("null", type);
    }
  }

  public boolean nextBoolean() {
    byte type = readByte();
    if (type != TYPE_BOOLEAN) {
      throw wrongTypeException("boolean", type);
    }
    return readByte() != 0;
  }

  private int nextIntInRange(String javaType, int min, int max) {
    int value = nextInt(javaType);
    if (min <= value && value <= max) {
      return value;
    } else {
      throw wrongNumberException(javaType, value);
    }
  }

  public byte nextByte() {
    return (byte) nextIntInRange("byte", Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  public char nextChar() {
    String str = nextString();
    if (str.length() != 1) {
      throw wrongStringException("char", str);
    }
    return str.charAt(0);
  }

  public short nextShort() {
    return (short) nextIntInRange("short", Short.MIN_VALUE, Short.MAX_VALUE);
  }

  public int nextInt() {
    return nextInt("int");
  }

  private int nextInt(String javaType) {
    byte type = readByte();
    switch (type) {
      case TYPE_INT:
        return readInt();
      case TYPE_DOUBLE:
        double value = readDouble();
        int result = (int) value;
        if (result != value) {
          throw wrongNumberException(javaType, value);
        }
        return result;
      default:
        throw wrongTypeException(javaType, type);
    }
  }

  public long nextLong() {
    byte type = readByte();
    switch (type) {
      case TYPE_INT:
        return readInt();
      case TYPE_DOUBLE:
        double value = readDouble();
        long result = (long) value;
        if (result != value) {
          throw wrongNumberException("long", value);
        }
        return result;
      default:
        throw wrongTypeException("long", type);
    }
  }

  public float nextFloat() {
    byte type = readByte();
    switch (type) {
      case TYPE_INT:
        return readInt();
      case TYPE_DOUBLE:
        return (float) readDouble();
      default:
        throw wrongTypeException("float", type);
    }
  }

  public double nextDouble() {
    byte type = readByte();
    switch (type) {
      case TYPE_INT:
        return readInt();
      case TYPE_DOUBLE:
        return readDouble();
      default:
        throw wrongTypeException("double", type);
    }
  }

  public String nextString() {
    byte type = readByte();
    if (type != TYPE_STRING) {
      throw wrongTypeException("String", type);
    }
    return readString();
  }

  void checkEOF() {
    if (offset != buffer.length) {
      throw new IllegalStateException("Not EOF");
    }
  }
}
