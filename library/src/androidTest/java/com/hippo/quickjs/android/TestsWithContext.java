package com.hippo.quickjs.android;

import org.junit.After;
import org.junit.Before;

public abstract class TestsWithContext {
    protected QuickJS quickJS;
    protected JSRuntime runtime;
    protected JSContext context;

    @Before
    public void setup() {
        quickJS = new QuickJS.Builder().build();
        runtime = quickJS.createJSRuntime();
        context = runtime.createJSContext();
    }

    @After
    public void cleanup() {
        if (context != null) {
            context.close();
            context = null;
        }
        if (runtime != null) {
            runtime.close();
            runtime = null;
        }
        quickJS = null;
    }
}
