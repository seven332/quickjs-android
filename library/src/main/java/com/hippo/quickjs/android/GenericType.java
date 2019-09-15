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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Represents a generic type {@code T}.
 */
public class GenericType<T> implements Type {

  final Type type;

  /**
   * Create a GenericType with the type.
   * It's unsafe. The type and the generic type {@code T} must match.
   */
  public GenericType(Type type) {
    this.type = Types.canonicalize(type);
  }

  protected GenericType() {
    Type superclass = getClass().getGenericSuperclass();
    if (!(superclass instanceof ParameterizedType)) {
      throw new RuntimeException("Missing type parameter");
    }
    ParameterizedType parameterized = (ParameterizedType) superclass;
    type = Types.canonicalize(parameterized.getActualTypeArguments()[0]);
  }
}
