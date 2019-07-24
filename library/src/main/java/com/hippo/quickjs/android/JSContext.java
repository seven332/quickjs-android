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

import java.io.Closeable;

public class JSContext implements Closeable {

  private long context;
  private NativeCleaner<JSValue> cleaner;

  JSContext(long context) {
    this.context = context;
    this.cleaner = new JSValueCleaner();
  }

  private void checkClosed() {
    if (context == 0) {
      throw new IllegalStateException("The JSContext is closed");
    }
  }

  public synchronized <T> T evaluate(String script, String fileName, Class<T> returnType) {
    return evaluate(script, fileName, QuickJS.EVAL_TYPE_GLOBAL, 0, returnType);
  }

  public synchronized <T> T evaluate(String script, String fileName, int type, int flags, Class<T> returnType) {
    checkClosed();

    if (type != QuickJS.EVAL_TYPE_GLOBAL && type != QuickJS.EVAL_TYPE_MODULE) {
      throw new QuickJSException("Invalid type: " + type);
    }
    if ((flags & (~QuickJS.EVAL_FLAG_MASK)) != 0) {
      throw new QuickJSException("Invalid flags: " + flags);
    }

    long value = QuickJS.evaluate(context, script, fileName, type | flags);

    if (value == 0) {
      throw new QuickJSException("Fail to evaluate the script");
    }

    JSValue jsValue = new JSValue(this, value);
    cleaner.register(jsValue, value);

    // Trigger cleaner
    cleaner.clean();

    // TODO convert the js value to return type
    return null;
  }

  synchronized void destroyValue(long value) {
    checkClosed();
    QuickJS.destroyValue(context, value);
  }

  synchronized int notRemovedJSValueCount() {
    return cleaner.size();
  }

  @Override
  public synchronized void close() {
    if (context != 0) {
      cleaner.forceClean();
      long contextToClose = context;
      context = 0;
      QuickJS.destroyContext(contextToClose);
    }
  }

  private class JSValueCleaner extends NativeCleaner<JSValue> {

    @Override
    public void onRemove(long pointer) {
      destroyValue(pointer);
    }
  }
}
