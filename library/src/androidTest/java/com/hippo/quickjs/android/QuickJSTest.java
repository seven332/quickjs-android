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

import org.junit.Ignore;
import org.junit.Test;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.junit.Assert.assertNotEquals;

public class QuickJSTest {

  @Test
  public void testCreateRuntime() {
    long runtime = QuickJS.createRuntime();
    assertNotEquals(0, runtime);
    QuickJS.destroyRuntime(runtime);
  }

  @Test
  public void testDestroyRuntime() {
    assertException(
        IllegalStateException.class,
        "Null JSRuntime",
        () -> QuickJS.destroyRuntime(0)
    );
  }

  private void withRuntime(WithRuntimeBlock block) {
    long runtime = QuickJS.createRuntime();
    assertNotEquals(0, runtime);
    try {
      block.run(runtime);
    } finally {
      QuickJS.destroyRuntime(runtime);
    }
  }

  private interface WithRuntimeBlock {
    void run(long runtime);
  }

  @Ignore("It causes accessing null pointer in C")
  @Test
  public void testOutOfMemory() {
    for (int i = 0; i < 50000; i++) {
      int mallocLimit = i;
      withRuntime(runtime -> {
        QuickJS.setRuntimeMallocLimit(runtime, mallocLimit);
        assertException(
            IllegalStateException.class,
            "Out of memory",
            () -> QuickJS.createContext(runtime)
        );
      });
    }
  }
}
