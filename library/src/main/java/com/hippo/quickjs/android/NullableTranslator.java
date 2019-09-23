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

import java.lang.reflect.Type;

class NullableTranslator<T> extends Translator<T> {

  public static final Factory FACTORY = (depot, type) -> {
    if (Types.isNonNull(type)) return null;
    Type nonNullType = Types.nonNullOf(type);
    Translator<Object> nonNullTranslator = depot.getTranslator(nonNullType);
    return create(nonNullType, nonNullTranslator);
  };

  private final Translator<T> delegate;

  private NullableTranslator(
      byte[] pickleCommand,
      byte[] unpickleCommand,
      Placeholder[] placeholders,
      Translator<T> delegate) {
    super(pickleCommand, unpickleCommand, placeholders);
    this.delegate = delegate;
  }

  @Override
  protected T unpickle(JSContext context, BitSource source) {
    if (source.readBoolean()) {
      return delegate.unpickle(context, source);
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void pickle(JSContext context, Object value, BitSink sink) {
    if (value == null) {
      sink.writeBoolean(false);
    } else {
      sink.writeBoolean(true);
      delegate.pickle(context, (T) value, sink);
    }
  }

  private static <T> Translator<T> createInvoke(Type nonNullType, Translator<T> nonNullTranslator) {
    byte[] pickleCommand = new byte[1 + 4 + 1 + 8];
    pickleCommand[0] = PICKLE_FLAG_ATTR_NULLABLE;
    Bits.writeInt(pickleCommand, 1, 1 + 8);
    pickleCommand[1 + 4] = PICKLE_FLAG_TYPE_COMMAND;

    byte[] unpickleCommand = new byte[1 + 4 + 1 + 8];
    unpickleCommand[0] = UNPICKLE_FLAG_ATTR_NULLABLE;
    Bits.writeInt(unpickleCommand, 1, 1 + 8);
    unpickleCommand[1 + 4] = UNPICKLE_FLAG_TYPE_COMMAND;

    Placeholder[] placeholders = new Placeholder[] {
        new Placeholder(nonNullType, 1 + 4 + 1, 1 + 4 + 1)
    };

    return new NullableTranslator<>(
        pickleCommand,
        unpickleCommand,
        placeholders,
        nonNullTranslator
    );
  }

  private static <T> Translator<T> createInline(Translator<T> translator) {
    byte[] pickleCommand = translator.pickleCommand;
    byte[] newPickleCommand = new byte[1 + 4 + pickleCommand.length];
    newPickleCommand[0] = PICKLE_FLAG_ATTR_NULLABLE;
    Bits.writeInt(newPickleCommand, 1, pickleCommand.length);
    System.arraycopy(pickleCommand, 0, newPickleCommand, 1 + 4, pickleCommand.length);

    byte[] unpickleCommand = translator.unpickleCommand;
    byte[] newUnpickleCommand = new byte[1 + 4 + unpickleCommand.length];
    newUnpickleCommand[0] = UNPICKLE_FLAG_ATTR_NULLABLE;
    Bits.writeInt(newUnpickleCommand, 1, unpickleCommand.length);
    System.arraycopy(unpickleCommand, 0, newUnpickleCommand, 1 + 4, unpickleCommand.length);

    Placeholder[] placeholders = translator.placeholders;
    Placeholder[] newPlaceholders;
    if (placeholders.length == 0) {
      newPlaceholders = EMPTY_PLACEHOLDER_ARRAY;
    } else {
      newPlaceholders = new Placeholder[placeholders.length];
      for (int i = 0; i < placeholders.length; i++) {
        Placeholder placeholder = placeholders[i];
        newPlaceholders[i] = new Placeholder(
            placeholder.type,
            1 + 4 + placeholder.pickleIndex,
            1 + 4 + placeholder.unpickleIndex
        );
      }
    }

    return new NullableTranslator<>(newPickleCommand, newUnpickleCommand, newPlaceholders, translator);
  }

  private static <T> Translator<T> create(Type nonNullType, Translator<T> nonNullTranslator) {
    if (nonNullTranslator.pickleCommand.length > MAX_INLINE_SIZE ||
        nonNullTranslator.unpickleCommand.length > MAX_INLINE_SIZE) {
      return createInvoke(nonNullType, nonNullTranslator);
    } else {
      return createInline(nonNullTranslator);
    }
  }
}
