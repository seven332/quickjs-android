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

import androidx.annotation.Nullable;

/**
 * JavaScript function.
 */
public final class JSFunction extends JSObject {

  JSFunction(long pointer, JSContext jsContext) {
    super(pointer, jsContext, null);
  }

  /**
   * Calls the JavaScript function.
   */
  public JSValue invoke(@Nullable JSValue thisObj, JSValue[] args) {
    // Check whether JSValues are from the same JSRuntime
    if (thisObj != null) checkSameJSContext(thisObj);
    for (JSValue arg : args) checkSameJSContext(arg);

    long[] valueArgs = new long[args.length];
    for (int i = 0; i < args.length; i++) {
      valueArgs[i] = args[i].pointer;
    }

    synchronized (jsContext.jsRuntime) {
      long context = jsContext.checkClosed();
      long ret = QuickJS.invokeValueFunction(context, pointer, thisObj != null ? thisObj.pointer : 0, valueArgs);
      return jsContext.wrapAsJSValue(ret);
    }
  }
}
