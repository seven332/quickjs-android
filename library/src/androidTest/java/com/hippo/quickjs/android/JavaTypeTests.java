/*
 * Copyright 2021 Hippo Seven
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class JavaTypeTests {
  @Test
  public void getType() {
    Type type = new JavaType<List<Integer>>() {}.type;
    assertTrue(type instanceof ParameterizedType);

    ParameterizedType p = (ParameterizedType) type;
    assertEquals(p.getRawType(), List.class);
    assertArrayEquals(p.getActualTypeArguments(), new Type[] { Integer.class });
  }
}
