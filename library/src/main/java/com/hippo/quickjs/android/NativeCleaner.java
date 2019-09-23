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
abstract class NativeCleaner {

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private Set<NativeReference> phantomReferences = new HashSet<>();
  private ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

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
  public void register(Object referent, long pointer) {
    phantomReferences.add(new NativeReference(referent, pointer, referenceQueue));
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
    NativeReference ref;
    while ((ref = (NativeReference) referenceQueue.poll()) != null) {
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
    for (NativeReference ref : phantomReferences) {
      onRemove(ref.pointer);
    }
    phantomReferences.clear();
  }

  private static class NativeReference extends PhantomReference<Object> {

    private long pointer;

    private NativeReference(Object referent, long pointer, ReferenceQueue<? super Object> q) {
      super(referent, q);
      this.pointer = pointer;
    }
  }
}
