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

import java.util.List;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.junit.Assert.assertEquals;

public class GenericTypeTest {

  @Test
  public void test() {
    assertEquals(Types.arrayOf(String.class), new GenericType<String[]>() {}.type);
    assertEquals(Types.newParameterizedType(List.class, String.class), new GenericType<List<String>>() {}.type);
  }

  @Test
  public void exception() {
    assertException(
        RuntimeException.class,
        "Missing type parameter",
        GenericType::new
    );
  }
}
