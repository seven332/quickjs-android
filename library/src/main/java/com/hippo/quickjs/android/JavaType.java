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

import androidx.annotation.NonNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class JavaType<T> {
  @NonNull
  public Type type;

  public JavaType() {
    Type supertype = JavaTypes.canonicalize(getClass().getGenericSuperclass());
    if (!(supertype instanceof ParameterizedType)) invalidJavaType();

    Type[] types = ((ParameterizedType) supertype).getActualTypeArguments();
    if (types.length != 1) invalidJavaType();

    type = types[0];
  }

  private void invalidJavaType() {
    throw new IllegalStateException(
        "Invalid JavaType. JavaType must be inherited by a anonymous class");
  }
}
