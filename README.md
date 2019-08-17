# QuickJS Android

[QuickJS](https://bellard.org/quickjs/) Android wrapper.

## Usage

### Evaluate Scripts

```Java
QuickJS quickJS = new QuickJS.Builder().build();
try (JSRuntime runtime = quickJS.createJSRuntime()) {
  try (JSContext context = runtime.createJSContext()) {
    String script1 = "" +
        "function fibonacci(n) {" +
        "  if (n == 0 || n == 1) return n;" +
        "  return fibonacci(n - 1) + fibonacci(n - 2);" +
        "}";
    // Evaluate a script without return value
    context.evaluate(script1, "fibonacci.js");

    String script2 = "fibonacci(10);";
    // Evaluate a script with return value
    int result = context.evaluate(script2, "fibonacci.js", int.class);
    assertEquals(55, result);
  }
}
```

## Test

The original tests and benchmarks of QuickJS are in [android-test](android-test). It's a console-like app running all tests and benchmarks at startup, like `make test`.
