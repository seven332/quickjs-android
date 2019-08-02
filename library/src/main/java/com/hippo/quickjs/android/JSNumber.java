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

/**
 * JavaScript number.
 */
public abstract class JSNumber extends JSValue {

  JSNumber(long pointer, JSContext jsContext) {
    super(pointer, jsContext);
  }

  /**
   * Returns byte value.
   *
   * @throws JSDataException if it has decimal part, or bigger than {@link Byte#MAX_VALUE},
   *         or smaller than {@link Byte#MIN_VALUE}
   */
  public abstract byte getByte();

  /**
   * Returns short value.
   *
   * @throws JSDataException if it has decimal part, or bigger than {@link Short#MAX_VALUE},
   *         or smaller than {@link Short#MIN_VALUE}
   */
  public abstract short getShort();

  /**
   * Return int value.
   *
   * @throws JSDataException if it has decimal part, or bigger than {@link Integer#MAX_VALUE},
   *         or smaller than {@link Integer#MIN_VALUE}
   */
  public abstract int getInt();

  /**
   * Return long value.
   *
   * @throws JSDataException if it has decimal part, or bigger than {@link Long#MAX_VALUE},
   *         or smaller than {@link Long#MIN_VALUE}
   */
  public abstract long getLong();

  /**
   * Return float value.
   */
  public abstract float getFloat();

  /**
   * Return double value.
   */
  public abstract double getDouble();
}
