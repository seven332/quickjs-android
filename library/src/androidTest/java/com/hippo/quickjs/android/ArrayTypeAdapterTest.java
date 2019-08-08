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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayTypeAdapterTest {

  @Test
  public void convert() {
    QuickJS quickJS = new QuickJS.Builder().build();
    try (JSRuntime runtime = quickJS.createJSRuntime()) {
      try (JSContext context = runtime.createJSContext()) {

        ArrayPipe pipe = context.evaluate("" +
            "a = {\n" +
            "  intArray: function(a) { return a },\n" +
            "  integerArray: function(a) { return a },\n" +
            "  intArray2: function(a) { return a },\n" +
            "  stringArray: function(a) { return a },\n" +
            "}", "test.js", ArrayPipe.class);

        assertThat(pipe.intArray(new int[] {1, 2, 3})).containsExactly(1, 2, 3);
        assertThat(pipe.intArray(new int[] {})).isEmpty();
        assertThat(pipe.intArray(null)).isNull();
        assertThat(pipe.integerArray(new Integer[] {1, 2, null})).containsExactly(1, 2, null);
        assertThat(pipe.integerArray(new Integer[] {})).isEmpty();
        assertThat(pipe.integerArray(null)).isNull();
        assertThat(pipe.intArray2(new int[][] {new int[] {1, 2}, null, new int[] {}})).containsExactly(new int[] {1, 2}, null, new int[] {});
        assertThat(pipe.intArray2(new int[][] {})).isEmpty();
        assertThat(pipe.intArray2(null)).isNull();
        assertThat(pipe.stringArray(new String[] {"str", "ing", null})).containsExactly("str", "ing", null);
        assertThat(pipe.stringArray(new String[] {})).isEmpty();
        assertThat(pipe.stringArray(null)).isNull();
      }
    }
  }

  interface ArrayPipe {
    int[] intArray(int[] a);
    Integer[] integerArray(Integer[] a);
    int[][] intArray2(int[][] a);
    String[] stringArray(String[] a);
  }
}
