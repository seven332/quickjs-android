package com.hippo.quickjs.android;

import org.junit.Test;

import static com.hippo.quickjs.android.Utils.assertException;
import static org.assertj.core.api.Assertions.assertThat;

public class JSFunctionCallbackTest extends TestsWithContext {

  @Test
  public void invoke() {
    JSValue plusFunction = context.createJSFunction((context, args) -> {
      int a = args[0].cast(JSNumber.class).getInt();
      int b = args[1].cast(JSNumber.class).getInt();
      int sum = a + b;
      return context.createJSNumber(sum);
    });

    context.getGlobalObject().setProperty("plus", plusFunction);
    int result = context.evaluate("plus(1, 2)", "test.js", Integer.class);
    assertThat(result).isEqualTo(3);
  }

  @Test
  public void invoke_exception() {
    JSValue plusFunction = context.createJSFunction((context, args) -> {
      int x = args[0].cast(JSNumber.class).getInt();
      return context.createJSNumber(x);
    });

    context.getGlobalObject().setProperty("x", plusFunction);
    assertException(
      JSEvaluationException.class,
      "InternalError: Catch java exception\n    at <eval> (test.js)\n",
      () -> context.evaluate("x()", "test.js", Integer.class)
    );
  }
}
