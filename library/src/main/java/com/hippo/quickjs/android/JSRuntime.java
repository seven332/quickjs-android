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

// TODO Check all JSContext closed when closing JSRuntime

/**
 * JSRuntime is a JavaScript runtime with a memory heap.
 * It can't evaluate JavaScript script.
 *
 * @see JSContext
 */
public class JSRuntime implements Closeable {

  private long pointer;
  private final QuickJS quickJS;

  JSRuntime(long pointer, QuickJS quickJS) {
    this.pointer = pointer;
    this.quickJS = quickJS;
  }

  private void checkClosed() {
    if (pointer == 0) {
      throw new IllegalStateException("The JSRuntime is closed");
    }
  }

  /**
   * Set the malloc limit for this JSRuntime.
   * Only positive number and {@code -1} are accepted.
   * {@code -1} for no limit.
   */
  public synchronized void setMallocLimit(int mallocLimit) {
    checkClosed();

    if (mallocLimit == 0 || mallocLimit < -1) {
      throw new IllegalArgumentException("Only positive number and -1 are accepted as malloc limit");
    }

    QuickJS.setRuntimeMallocLimit(pointer, mallocLimit);
  }

  /**
   * Creates a JSContext with the memory heap of this JSRuntime.
   */
  public synchronized JSContext createJSContext() {
    checkClosed();
    long context = QuickJS.createContext(pointer);
    if (context == 0) {
      throw new IllegalStateException("Cannot create JSContext instance");
    }
    return new JSContext(context, quickJS, this);
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
