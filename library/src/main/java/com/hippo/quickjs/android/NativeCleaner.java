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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;

/**
 * https://youtu.be/7_caITSjk1k
 */
abstract class NativeCleaner<T> {

  private final Set<NativeReference<T>> phantomReferences = new HashSet<>();
  private final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();

  /**
   * Returns the size of not removed objects.
   */
  public int size() {
    return phantomReferences.size();
  }

  /**
   * Registers the object and the native pointer to this cleaner.
   *
   * @param referent the object
   * @param pointer the native pointer
   */
  public void register(T referent, long pointer) {
    phantomReferences.add(new NativeReference<>(referent, pointer, referenceQueue));
  }

  /**
   * Releases the native resources associated with the native pointer.
   * It's called in {@link #clean()} on objects recycled by GC,
   * or in {@link #forceClean()} on all objects.
   * It's only called once on each object.
   *
   * @param pointer the native pointer
   */
  public abstract void onRemove(long pointer);

  /**
   * Calls {@link #onRemove(long)} on objects recycled by GC.
   */
  @SuppressWarnings("unchecked")
  public void clean() {
    NativeReference<T> ref;
    while ((ref = (NativeReference<T>) referenceQueue.poll()) != null) {
      if (phantomReferences.contains(ref)) {
        onRemove(ref.pointer);
        phantomReferences.remove(ref);
      }
    }
  }

  /**
   * Calls {@link #onRemove(long)} on all objects.
   */
  public void forceClean() {
    for (NativeReference<T> ref : phantomReferences) {
      onRemove(ref.pointer);
    }
    phantomReferences.clear();
  }

  private static class NativeReference<T> extends PhantomReference<T> {

    private final long pointer;

    private NativeReference(T referent, long pointer, ReferenceQueue<? super T> q) {
      super(referent, q);
      this.pointer = pointer;
    }
  }
}
