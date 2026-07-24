package com.stepcounter.app;

/**
 * Manual driver (not JUnit) verifying the auto-generated export file names for the export UX
 * feature: "{repo}-{branch}.html" for full-mode results, "{repo}-{branchA}-vs-{branchB}.html"
 * for diff-mode results.
 */
public final class FileNameGenerationManualCheck {

    public static void main(String[] args) {
        check(StepCounterApp.repoShortName("https://example.com/acme-orders.git"), "acme-orders");
        check(StepCounterApp.repoShortName("https://example.com/acme-orders"), "acme-orders");
        check(StepCounterApp.repoShortName("https://example.com/acme-orders.git/"), "acme-orders");
        check(StepCounterApp.sanitizeFileNamePart("feature/order-refactor"), "feature-order-refactor");
        check(StepCounterApp.sanitizeFileNamePart("main"), "main");

        String fullName = StepCounterApp.repoShortName("https://example.com/acme-orders.git") + "-"
                + StepCounterApp.sanitizeFileNamePart("main") + ".html";
        check(fullName, "acme-orders-main.html");

        String diffName = StepCounterApp.repoShortName("https://example.com/acme-orders.git") + "-"
                + StepCounterApp.sanitizeFileNamePart("main") + "-vs-"
                + StepCounterApp.sanitizeFileNamePart("feature/order-refactor") + ".html";
        check(diffName, "acme-orders-main-vs-feature-order-refactor.html");

        System.out.println("ALL OK");
    }

    private static void check(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
        System.out.println("OK  " + actual);
    }
}
