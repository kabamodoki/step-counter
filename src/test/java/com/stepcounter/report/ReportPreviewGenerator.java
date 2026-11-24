package com.stepcounter.report;

import com.stepcounter.model.CommitInfo;
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
        CsvReportGenerator csvGen = new CsvReportGenerator();

        List<FileStepResult> files = List.of(
                new FileStepResult("src/main/java/com/example/order/OrderService.java", 842, new CommitInfo("山田 太郎", "2026-11-20 18:42:07", "注文確定時の在庫引当をリトライ対応")),
                new FileStepResult("src/main/java/com/example/order/OrderController.java", 611),
                new FileStepResult("src/main/java/com/example/pricing/PricingEngine.java", 588, new CommitInfo("suzuki", "2026-10-02 09:15:33", "Fix rounding in PricingEngine, add \"tax, incl.\" note")),
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
        csvGen.write(csvGen.buildFullCsv(scan), Path.of(outDir, "preview-full.csv"));

        List<DiffFileResult> diffs = List.of(
                new DiffFileResult("src/main/java/com/example/order/OrderService.java", 842, 906, 128, 64, new CommitInfo("山田 太郎", "2026-11-20 18:42:07", "注文確定時の在庫引当をリトライ対応")),
                new DiffFileResult("src/main/java/com/example/pricing/PricingEngine.java", 588, 631, 52, 9, new CommitInfo("suzuki", "2026-10-02 09:15:33", "Fix rounding in PricingEngine, add \"tax, incl.\" note")),
                new DiffFileResult("src/main/java/com/example/order/OrderValidator.java", 176, 145, 0, 31)
        );
        DiffResult diff = new DiffResult(
                "https://example.com/acme-orders.git", "main", "feature/order-refactor", "Java",
                StepMode.FULL, List.of("target/", "src/test/", "src/main/resources/"), diffs);
        gen.write(gen.buildDiffReport(diff), Path.of(outDir, "preview-diff.html"));
        csvGen.write(csvGen.buildDiffCsv(diff), Path.of(outDir, "preview-diff.csv"));

        System.out.println("wrote preview-full/diff .html and .csv to " + outDir);
    }
}
