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

import androidx.annotation.Nullable;

/**
 * This exception is raised if QuickJS raises a JavaScript exception.
 */
public class JSEvaluationException extends RuntimeException {

  private boolean isError;
  private String exception;
  private String stack;

  JSEvaluationException(JSException jsException) {
    this(jsException.isError(), jsException.getException(), jsException.getStack());
  }

  private JSEvaluationException(boolean isError, String exception, String stack) {
    super(toMessage(isError, exception, stack));
    this.isError = isError;
    this.exception = exception;
    this.stack = stack;
  }

  public boolean isError() {
    return isError;
  }

  /**
   * The exception message.
   */
  @Nullable
  public String getException() {
    return exception;
  }

  /**
   * The stack trace.
   */
  @Nullable
  public String getStack() {
    return stack;
  }

  private static String toMessage(boolean isError, String exception, String stack) {
    StringBuilder sb = new StringBuilder();
    if (!isError) {
      sb.append("Throw: ");
    }
    sb.append(exception).append("\n");
    if (stack != null) {
      sb.append(stack);
    }
    return sb.toString();
  }
}
