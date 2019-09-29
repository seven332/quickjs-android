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

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * QuickJS is a resources container to create {@link JSRuntime}s.
 */
public class QuickJS implements Translator.Depot, Closeable {

  private static final List<Translator.Factory> BUILT_IN_FACTORIES = new ArrayList<>(4);

  static {
    BUILT_IN_FACTORIES.add(NullableTranslator.FACTORY);
    BUILT_IN_FACTORIES.add(StandardTranslators.FACTORY);
    BUILT_IN_FACTORIES.add(ArrayTranslator.FACTORY);
    BUILT_IN_FACTORIES.add(InterfaceTranslator.FACTORY);
  }

  private final List<Translator.Factory> factories;
  private final Map<Type, Translator<?>> translatorCache;

  private QuickJS(QuickJS.Builder builder) {
    List<Translator.Factory> factories = new ArrayList<>(builder.factories.size() + BUILT_IN_FACTORIES.size());
    factories.addAll(builder.factories);
    factories.addAll(BUILT_IN_FACTORIES);
    this.factories = Collections.unmodifiableList(factories);
    this.translatorCache = new HashMap<>();
  }

  private Translator<?> getUncachedTranslator(Type type) {
    for (int i = 0, size = factories.size(); i < size; i++) {
      Translator<?> translator = factories.get(i).create(this, type);
      if (translator != null) {
        return translator;
      }
    }
    return null;
  }

  private void cacheTranslator(Type type, Translator<?> translator) {
    Stack<Translator<?>> stack = new Stack<>();
    stack.add(translator);

    Map<Type, Translator<?>> toCache = new HashMap<>();
    toCache.put(type, translator);

    // Get all translators related to this translator
    while (!stack.isEmpty()) {
      Translator<?> tr = stack.pop();
      for (Translator.Placeholder ph : tr.placeholders) {
        Type t = ph.type;
        if (translatorCache.containsKey(t) || toCache.containsKey(t)) {
          continue;
        }
        Translator<?> newTr = getUncachedTranslator(t);
        if (newTr == null) {
          throw new IllegalStateException("Can't get translator for type: " + type);
        }
        stack.push(newTr);
        toCache.put(t, newTr);
      }
    }

    // Push all commands
    int toCacheSize = toCache.size();
    byte[][] commands = new byte[toCacheSize * 2][];
    Iterator<Translator<?>> iterator = toCache.values().iterator();
    for (int i = 0; i < toCacheSize; i++) {
      Translator<?> tr = iterator.next();
      commands[i * 2] = tr.pickleCommand;
      commands[i * 2 + 1] = tr.unpickleCommand;
    }
    long[] pointers = QuickJS.pushCommands(commands);
    iterator = toCache.values().iterator();
    for (int i = 0; i < toCacheSize; i++) {
      Translator<?> tr = iterator.next();
      tr.picklePointer = pointers[i * 2];
      tr.unpicklePointer = pointers[i * 2 + 1];
    }

    // Back fill
    translatorCache.putAll(toCache);
    iterator = toCache.values().iterator();
    while (iterator.hasNext()) {
      Translator<?> tr = iterator.next();
      for (Translator.Placeholder placeholder : tr.placeholders) {
        Translator<?> child = translatorCache.get(placeholder.type);
        if (child == null) {
          throw new RuntimeException("Internal error: Can't get translator for type: " + placeholder.type);
        }
        Bits.writeLong(tr.pickleCommand, placeholder.pickleIndex, child.picklePointer);
        Bits.writeLong(tr.unpickleCommand, placeholder.unpickleIndex, child.unpicklePointer);
      }
    }
    QuickJS.updateCommands(pointers, commands);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Translator<T> getTranslator(Type type) {
    // Canonicalize type
    Type newType = Types.removeSubtypeWildcard(Types.canonicalize(type));

    Translator<?> translator = translatorCache.get(newType);
    if (translator != null) {
      return (Translator<T>) translator;
    }

    translator = getUncachedTranslator(type);
    if (translator != null) {
      cacheTranslator(type, translator);
      return (Translator<T>) translator;
    }

    throw new IllegalArgumentException("Can't find Translator for " + type);
  }

  /**
   * Creates a JSRuntime with resources in this QuickJS.
   */
  public JSRuntime createJSRuntime() {
    long runtime = QuickJS.createRuntime();
    if (runtime == 0) {
      throw new IllegalStateException("Cannot create JSRuntime instance");
    }
    return new JSRuntime(runtime, this);
  }

  @Override
  public void close() {
    // TODO Check all JSRuntime closed
    int translatorCacheSize = translatorCache.size();
    long[] pointers = new long[translatorCacheSize * 2];
    Iterator<Translator<?>> iterator = translatorCache.values().iterator();
    for (int i = 0; i < translatorCacheSize; i++) {
      Translator<?> translator = iterator.next();
      pointers[i * 2] = translator.picklePointer;
      pointers[i * 2 + 1] = translator.unpicklePointer;
    }

    QuickJS.popCommands(pointers);

    for (Translator<?> translator : translatorCache.values()) {
      translator.picklePointer = 0;
      translator.unpicklePointer = 0;
    }
  }

  public static class Builder {

    private List<Translator.Factory> factories = new ArrayList<>();

    public <T> Builder addTranslator(Type type, Translator<T> translator) {
      return addTranslatorFactory((depot, targetType) -> {
        if (Types.equals(type, targetType)) {
          return translator;
        }
        return null;
      });
    }

    public Builder addTranslatorFactory(Translator.Factory factory) {
      factories.add(factory);
      return this;
    }

    public QuickJS build() {
      return new QuickJS(this);
    }
  }

  static {
    System.loadLibrary("quickjs-android");
  }

  static native long createRuntime();
  static native void setRuntimeMallocLimit(long runtime, int mallocLimit);
  static native void setRuntimeInterruptHandler(long runtime, JSRuntime.InterruptHandler interruptHandler);
  static native void destroyRuntime(long runtime);

  static native long createContext(long runtime);
  static native void destroyContext(long context);

  static native void setContextValue(long context, String name, long unpickleCommand, byte[] bytes, int byteSize);

  static native byte[] invokeValueFunction(long context, long value, String name, long[] unpickleCommands, byte[][] argContexts, int[] argContextSizes, long pickleCommand, boolean required);

  static native void destroyValue(long context, long value);

  static native byte[] evaluate(long context, String sourceCode, String fileName, int flags, long pickle);

  static native long pushCommand(byte[] command);
  static native long[] pushCommands(byte[][] commands);
  static native void updateCommands(long[] pointers, byte[][] commands);
  static native void popCommands(long[] pointers);
}
