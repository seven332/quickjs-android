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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayTypeAdapterTest extends TestsWithContext {
  private ArrayPipe pipe;

  @Before
  @Override
  public void setup() {
    super.setup();
    pipe = context.evaluate("" +
      "a = {\n" +
      "  intArray: function(a) { return a },\n" +
      "  integerArray: function(a) { return a },\n" +
      "  intIntArray: function(a) { return a },\n" +
      "  stringArray: function(a) { return a },\n" +
      "}", "test.js", ArrayPipe.class);
  }

  @Test
  public void intArray() {
    assertThat(pipe.intArray(new int[] {1, 2, 3})).containsExactly(1, 2, 3);
  }

  @Test
  public void intArray_empty() {
    assertThat(pipe.intArray(new int[] {})).isEmpty();
  }

  @Test
  public void intArray_null() {
    assertThat(pipe.intArray(null)).isNull();
  }

  @Test
  public void integerArray() {
    assertThat(pipe.integerArray(new Integer[] {1, 2, null})).containsExactly(1, 2, null);
  }

  @Test
  public void integerArray_empty() {
    assertThat(pipe.integerArray(new Integer[] {})).isEmpty();
  }

  @Test
  public void integerArray_null() {
    assertThat(pipe.integerArray(null)).isNull();
  }

  @Test
  public void intIntArray() {
    assertThat(pipe.intIntArray(new int[][] {new int[] {1, 2}, null, new int[] {}}))
      .containsExactly(new int[] {1, 2}, null, new int[] {});
  }

  @Test
  public void intIntArray_empty() {
    assertThat(pipe.intIntArray(new int[][] {})).isEmpty();
  }

  @Test
  public void intIntArray_null() {
    assertThat(pipe.intIntArray(null)).isNull();
  }

  @Test
  public void stringArray() {
    assertThat(pipe.stringArray(new String[] {"str", "ing", null}))
      .containsExactly("str", "ing", null);
  }

  @Test
  public void stringArray_empty() {
    assertThat(pipe.stringArray(new String[] {})).isEmpty();
  }

  @Test
  public void stringArray_null() {
    assertThat(pipe.stringArray(null)).isNull();
  }

  private interface ArrayPipe {
    int[] intArray(int[] a);
    Integer[] integerArray(Integer[] a);
    int[][] intIntArray(int[][] a);
    String[] stringArray(String[] a);
  }
}
