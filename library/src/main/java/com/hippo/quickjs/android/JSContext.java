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

  JSContext(long context) {
    this.context = context;
  }

  private void checkClosed() {
    if (context == 0) {
      throw new IllegalStateException("The JSContext is closed");
    }
  }

  public synchronized Object evaluate(String script, String fileName, int flags) {
    checkClosed();
    return QuickJS.evaluate(context, script, fileName, flags);
  }

  public synchronized void close() {
    if (context != 0) {
      long contextToClose = context;
      context = 0;
      QuickJS.destroyContext(contextToClose);
    }
  }
}
