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

// TODO Make JSValue closeable?

/**
 * JSValue is a Javascript value.
 * It could be a number, a object, null, undefined or something else.
 */
public abstract class JSValue {

  final long pointer;
  final JSContext jsContext;

  JSValue(long pointer, JSContext jsContext) {
    this.pointer = pointer;
    this.jsContext = jsContext;
  }

  /**
   * Cast this JSValue to a special type.
   *
   * @throws JSDataException if it's not the type
   */
  @SuppressWarnings("unchecked")
  public final <T extends JSValue> T cast(Class<T> clazz) {
    if (clazz.isInstance(this)) {
      return (T) this;
    } else {
      throw new JSDataException("expected: " + clazz.getSimpleName() + ", actual: " + getClass().getSimpleName());
    }
  }
}
