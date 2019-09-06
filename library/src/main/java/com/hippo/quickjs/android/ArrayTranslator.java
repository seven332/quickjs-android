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

import java.lang.reflect.Array;
import java.lang.reflect.Type;

class ArrayTranslator extends Translator<Object> {

  public static final Factory FACTORY = (depot, type) -> {
    Type elementType = Types.arrayComponentType(type);
    if (elementType == null) return null;
    Class<?> elementClass = Types.getRawType(elementType);
    Translator<Object> elementTranslator = depot.getTranslator(elementType);
    return ArrayTranslator.create(elementClass, elementTranslator).nullable();
  };

  private final Class<?> elementClass;
  private final Translator<Object> elementTranslator;

  private ArrayTranslator(
      byte[] pickleCommand,
      byte[] unpickleCommand,
      Placeholder[] placeholders,
      Class<?> elementClass,
      Translator<Object> elementTranslator
  ) {
    super(pickleCommand, unpickleCommand, placeholders);
    this.elementClass = elementClass;
    this.elementTranslator = elementTranslator;
  }

  @Override
  protected Object unpickle(BitSource source) {
    int length = source.nextInt();
    Object result = Array.newInstance(elementClass, length);
    for (int i = 0; i < length; i++) {
      Array.set(result, i, elementTranslator.unpickle(source));
    }
    return result;
  }

  @Override
  protected void pickle(Object value, BitSink sink) {
    throw new IllegalStateException("TODO");
  }

  private static Translator<Object> create(
      Class<?> elementClass,
      Translator<Object> elementTranslator
  ) {
    byte[] pickleCommand = elementTranslator.pickleCommand;
    byte[] newPickleCommand = new byte[1 + 4 + pickleCommand.length];
    newPickleCommand[0] = PICKLE_FLAG_TYPE_ARRAY;
    Bits.writeInt(newPickleCommand, 1, pickleCommand.length);
    System.arraycopy(pickleCommand, 0, newPickleCommand, 1 + 4, pickleCommand.length);

    byte[] newUnpickleCommand = new byte[0]; // TODO

    Placeholder[] placeholders = elementTranslator.placeholders;
    Placeholder[] newPlaceholders = new Placeholder[placeholders.length];
    for (int i = 0; i < placeholders.length; i++) {
      Placeholder placeholder = placeholders[i];
      newPlaceholders[i] = new Placeholder(
          placeholder.type,
          1 + 4 + placeholder.pickleIndex,
          0 // TODO
      );
    }

    return new ArrayTranslator(
        newPickleCommand,
        newUnpickleCommand,
        newPlaceholders,
        elementClass,
        elementTranslator
    );
  }
}
