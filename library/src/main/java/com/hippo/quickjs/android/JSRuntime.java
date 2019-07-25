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

public class JSRuntime implements Closeable {

  public static JSRuntime create() {
    long runtime = QuickJS.createRuntime();
    if (runtime == 0) {
      throw new IllegalStateException("Cannot create JSRuntime instance");
    }
    return new JSRuntime(runtime);
  }

  private long runtime;

  private JSRuntime(long runtime) {
    this.runtime = runtime;
  }

  private void checkClosed() {
    if (runtime == 0) {
      throw new IllegalStateException("The JSRuntime is closed");
    }
  }

  public synchronized JSContext createContext() {
    checkClosed();
    long context = QuickJS.createContext(runtime);
    if (context == 0) {
      throw new IllegalStateException("Cannot create JSContext instance");
    }
    return new JSContext(context);
  }

  @Override
  public synchronized void close() {
    if (runtime != 0) {
      long runtimeToClose = runtime;
      runtime = 0;
      QuickJS.destroyRuntime(runtimeToClose);
    }
  }
}