package com.stepcounter.report;

import com.stepcounter.model.FileStepResult;
import com.stepcounter.model.ScanResult;
import com.stepcounter.model.StepMode;

import java.util.List;

/** Manual driver (not JUnit) verifying CSV field escaping for paths with commas/quotes. */
public final class CsvReportGeneratorManualCheck {

    public static void main(String[] args) {
        CsvReportGenerator gen = new CsvReportGenerator();
        List<FileStepResult> files = List.of(
                new FileStepResult("src/main/java/App.java", 10),
                new FileStepResult("src/main/java/\"Weird, Name\".java", 5)
        );
        ScanResult scan = new ScanResult("https://example.com/repo.git", "main", "Java", StepMode.FULL,
                List.of(), files, 15);

        String csv = gen.buildFullCsv(scan);
        System.out.println(csv);

        if (!csv.contains("\"src/main/java/\"\"Weird, Name\"\".java\",5")) {
            throw new AssertionError("comma/quote escaping did not produce the expected CSV field");
        }
        if (!csv.startsWith("\uFEFF")) {
            throw new AssertionError("missing UTF-8 BOM at start of CSV");
        }
        System.out.println("OK");
    }
}
