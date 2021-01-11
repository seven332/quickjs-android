package com.hippo.quickjs.android;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

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

  @Test
  public void invoke_closure() {
    final AtomicReference<JSFunction> funcHolder = new AtomicReference<>();
    JSValue awaitFunction = context.createJSFunction((context, args) -> {
      funcHolder.set(args[0].cast(JSFunction.class));
      return context.createJSUndefined();
    });
    context.getGlobalObject().setProperty("await", awaitFunction);
    context.evaluate("x = 1; await(() => { x = x + 1 })", "test.js", Integer.class);
    funcHolder.get().invoke(null, new JSValue[] {});
    int result = context.getGlobalObject().getProperty("x").cast(JSNumber.class).getInt();
    assertThat(result).isEqualTo(2);
  }
}
