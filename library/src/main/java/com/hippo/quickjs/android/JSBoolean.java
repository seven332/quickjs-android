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
 * JavaScript boolean.
 */
public class JSBoolean extends JSValue {

  private final boolean value;

  JSBoolean(long pointer, JSContext jsContext, boolean value) {
    super(pointer, jsContext);
    this.value = value;
  }

  /**
   * Returns boolean value.
   */
  public boolean getBoolean() {
    return value;
  }
}
