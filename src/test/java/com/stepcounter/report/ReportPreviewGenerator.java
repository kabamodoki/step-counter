package com.stepcounter.report;

import com.stepcounter.model.DiffFileResult;
import com.stepcounter.model.DiffResult;
import com.stepcounter.model.FileStepResult;
import com.stepcounter.model.ScanResult;
import com.stepcounter.model.StepMode;

import java.nio.file.Path;
import java.util.List;

/**
 * Manual driver (not a JUnit test) used once during implementation to render the *real*
 * HtmlReportGenerator output for visual comparison against the approved mockup.
 * Not part of the automated build.
 */
public final class ReportPreviewGenerator {

    public static void main(String[] args) {
        HtmlReportGenerator gen = new HtmlReportGenerator();

        List<FileStepResult> files = List.of(
                new FileStepResult("src/main/java/com/example/order/OrderService.java", 842),
                new FileStepResult("src/main/java/com/example/order/OrderController.java", 611),
                new FileStepResult("src/main/java/com/example/pricing/PricingEngine.java", 588),
                new FileStepResult("src/main/java/com/example/inventory/InventoryClient.java", 402),
                new FileStepResult("src/main/java/com/example/order/OrderRepository.java", 355),
                new FileStepResult("src/main/java/com/example/notify/NotificationDispatcher.java", 298),
                new FileStepResult("src/main/java/com/example/order/OrderMapper.java", 210),
                new FileStepResult("src/main/java/com/example/order/OrderValidator.java", 176)
        );
        ScanResult scan = new ScanResult(
                "https://example.com/acme-orders.git", "main", "Java", StepMode.FULL,
                List.of("target/", "src/test/", "src/main/resources/"), files, 2910);

        String outDir = args.length > 0 ? args[0] : ".";
        gen.write(gen.buildFullReport(scan), Path.of(outDir, "preview-full.html"));

        List<DiffFileResult> diffs = List.of(
                new DiffFileResult("src/main/java/com/example/order/OrderService.java", 128, 64),
                new DiffFileResult("src/main/java/com/example/pricing/PricingEngine.java", 52, 9),
                new DiffFileResult("src/main/java/com/example/order/OrderValidator.java", 0, 31)
        );
        DiffResult diff = new DiffResult(
                "https://example.com/acme-orders.git", "main", "feature/order-refactor", "Java",
                StepMode.FULL, List.of("target/", "src/test/", "src/main/resources/"), diffs);
        gen.write(gen.buildDiffReport(diff), Path.of(outDir, "preview-diff.html"));

        System.out.println("wrote preview-full.html and preview-diff.html to " + outDir);
    }
}
