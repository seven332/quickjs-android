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

class Bits {

  static byte readByte(byte[] bytes, int offset) {
    return bytes[offset];
  }

  static short readShort(byte[] bytes, int offset) {
    return (short) (((bytes[offset + 1] & 0xff) <<  8) |
                    ((bytes[offset    ] & 0xff)      ));
  }

  static int readInt(byte[] bytes, int offset) {
    return (((bytes[offset + 3]       ) << 24) |
            ((bytes[offset + 2] & 0xff) << 16) |
            ((bytes[offset + 1] & 0xff) <<  8) |
            ((bytes[offset    ] & 0xff)      ));
  }

  static long readLong(byte[] bytes, int offset) {
    return ((((long) bytes[offset + 7]       ) << 56) |
            (((long) bytes[offset + 6] & 0xff) << 48) |
            (((long) bytes[offset + 5] & 0xff) << 40) |
            (((long) bytes[offset + 4] & 0xff) << 32) |
            (((long) bytes[offset + 3] & 0xff) << 24) |
            (((long) bytes[offset + 2] & 0xff) << 16) |
            (((long) bytes[offset + 1] & 0xff) <<  8) |
            (((long) bytes[offset    ] & 0xff)      ));
  }

  static double readDouble(byte[] bytes, int offset) {
    return Double.longBitsToDouble(readLong(bytes, offset));
  }

  static void writeByte(byte[] bytes, int offset, byte value) {
    bytes[offset] = value;
  }

  static void writeShort(byte[] bytes, int offset, short value) {
    bytes[offset + 1] = (byte) (value >>  8);
    bytes[offset    ] = (byte) (value      );
  }

  static void writeInt(byte[] bytes, int offset, int value) {
    bytes[offset + 3] = (byte) (value >> 24);
    bytes[offset + 2] = (byte) (value >> 16);
    bytes[offset + 1] = (byte) (value >>  8);
    bytes[offset    ] = (byte) (value      );
  }

  static void writeLong(byte[] bytes, int offset, long value) {
    bytes[offset + 7] = (byte) (value >> 56);
    bytes[offset + 6] = (byte) (value >> 48);
    bytes[offset + 5] = (byte) (value >> 40);
    bytes[offset + 4] = (byte) (value >> 32);
    bytes[offset + 3] = (byte) (value >> 24);
    bytes[offset + 2] = (byte) (value >> 16);
    bytes[offset + 1] = (byte) (value >>  8);
    bytes[offset    ] = (byte) (value      );
  }

  static void writeFloat(byte[] bytes, int offset, float value) {
    writeInt(bytes, offset, Float.floatToRawIntBits(value));
  }

  static void writeDouble(byte[] bytes, int offset, double value) {
    writeLong(bytes, offset, Double.doubleToRawLongBits(value));
  }
}
