package com.stepcounter.app;

/**
 * Plain (non-Application) entry point. The shaded fat-jar manifest points here instead of
 * directly at {@link StepCounterApp} so `java -jar step-counter.jar` doesn't trip the
 * "JavaFX runtime components are missing" classpath check some JVMs apply to Application subclasses.
 */
public final class MainLauncher {

    private MainLauncher() {
    }

    public static void main(String[] args) {
        StepCounterApp.main(args);
    }
}
