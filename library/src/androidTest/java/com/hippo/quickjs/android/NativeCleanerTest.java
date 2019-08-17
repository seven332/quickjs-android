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

import java.util.BitSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeCleanerTest {

  @Ignore("There is no guarantee that this test will pass")
  @Test
  public void test() {
    int objectCount = 1234;

    TestNativeCleaner<Object> cleaner = new TestNativeCleaner<>(objectCount);

    for (int i = 0; i < objectCount; i++) {
      cleaner.register(new Object(), i);
    }

    // Trigger GC
    Runtime.getRuntime().gc();
    Runtime.getRuntime().gc();

    cleaner.clean();
    cleaner.assertAllCleaned();
  }

  private class TestNativeCleaner<T> extends NativeCleaner<T> {

    private BitSet bitSet;

    public TestNativeCleaner(int count) {
      bitSet = new BitSet(count);
      bitSet.set(0, count);
    }

    @Override
    public void onRemove(long pointer) {
      assertTrue(bitSet.get((int) pointer));
      bitSet.clear((int) pointer);
    }

    public void assertAllCleaned() {
      BitSet mask = new BitSet(bitSet.length());
      // The last object may not be cleaned
      mask.set(0, bitSet.length() - 1);
      assertFalse(bitSet.intersects(mask));
    }
  }
}
