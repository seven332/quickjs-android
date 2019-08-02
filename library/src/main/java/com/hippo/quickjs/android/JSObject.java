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
 * JavaScript object.
 */
public class JSObject extends JSValue {

  JSObject(long pointer, JSContext jsContext) {
    super(pointer, jsContext);
  }

  /**
   * Returns the property as a JSValue.
   *
   * @throws JSEvaluationException if the cannot read property of this JSValue.
   */
  public JSValue getProperty(int index) {
    synchronized (jsContext.jsRuntime) {
      long context = jsContext.checkClosed();
      long property = QuickJS.getValueProperty(context, pointer, index);
      return jsContext.wrapAsJSValue(property);
    }
  }

  /**
   * Returns the property as a JSValue.
   *
   * @throws JSEvaluationException if the cannot read property of this JSValue.
   */
  public JSValue getProperty(String name) {
    synchronized (jsContext.jsRuntime) {
      long context = jsContext.checkClosed();
      long property = QuickJS.getValueProperty(context, pointer, name);
      return jsContext.wrapAsJSValue(property);
    }
  }
}
