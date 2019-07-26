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

class QuickJS {

  static {
    System.loadLibrary("quickjs-android");
  }

  /**
   * Global code.
   */
  public static final int EVAL_TYPE_GLOBAL = 0 << 0;

  /**
   * Module code.
   */
  public static final int EVAL_TYPE_MODULE = 1 << 0;

  /**
   * Skip first line beginning with '#!'.
   */
  public static final int EVAL_FLAG_SHEBANG = 1 << 2;

  /**
   * Force 'strict' mode.
   */
  public static final int EVAL_FLAG_STRICT = 1 << 3;

  /**
   * Force 'strip' mode.
   *
   * Remove the debug information (including the source code
   * of the functions) to save memory.
   */
  public static final int EVAL_FLAG_STRIP = 1 << 4;

  static final int EVAL_FLAG_MASK = 0b11100;

  static final int VALUE_TAG_SYMBOL = -8;
  static final int VALUE_TAG_STRING = -7;
  static final int VALUE_TAG_OBJECT = -1;
  static final int VALUE_TAG_INT = 0;
  static final int VALUE_TAG_BOOL = 1;
  static final int VALUE_TAG_NULL = 2;
  static final int VALUE_TAG_UNDEFINED = 3;
  static final int VALUE_TAG_EXCEPTION = 6;
  static final int VALUE_TAG_FLOAT64 = 7;

  static native long createRuntime();
  static native void destroyRuntime(long runtime);

  static native long createContext(long runtime);
  static native void destroyContext(long context);

  static native int getValueTag(long value);
  static native boolean getValueBoolean(long value);
  static native int getValueInt(long value);
  static native double getValueDouble(long value);
  static native String getValueString(long context, long value);
  static native void destroyValue(long context, long value);

  static native JSException getException(long context);

  static native long evaluate(long context, String sourceCode, String fileName, int flags);
}
