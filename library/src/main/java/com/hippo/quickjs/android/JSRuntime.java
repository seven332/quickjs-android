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

// TODO Share synchronize lock in JSContext, JSValue of the same JSRuntime
public class JSRuntime implements Closeable {

  private long pointer;
  private final TypeAdapter.Depot depot;

  JSRuntime(long pointer, TypeAdapter.Depot depot) {
    this.pointer = pointer;
    this.depot = depot;
  }

  private void checkClosed() {
    if (pointer == 0) {
      throw new IllegalStateException("The JSRuntime is closed");
    }
  }

  public synchronized JSContext createJSContext() {
    checkClosed();
    long context = QuickJS.createContext(pointer);
    if (context == 0) {
      throw new IllegalStateException("Cannot create JSContext instance");
    }
    return new JSContext(context, depot, this);
  }

  @Override
  public synchronized void close() {
    if (pointer != 0) {
      long runtimeToClose = pointer;
      pointer = 0;
      QuickJS.destroyRuntime(runtimeToClose);
    }
  }
}
