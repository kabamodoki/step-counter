package com.stepcounter.report;

import com.stepcounter.model.DiffFileResult;
import com.stepcounter.model.DiffResult;
import com.stepcounter.model.FileStepResult;
import com.stepcounter.model.ScanResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Builds simple CSV exports of scan/diff results for opening in a spreadsheet. */
public class CsvReportGenerator {

    private static final String BOM = "\uFEFF";
    private static final String NEWLINE = "\r\n";

    public String buildFullCsv(ScanResult result) {
        List<FileStepResult> sorted = result.getFiles().stream()
                .sorted((a, b) -> Long.compare(b.getSteps(), a.getSteps()))
                .toList();
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("ファイルパス,ステップ数").append(NEWLINE);
        for (FileStepResult f : sorted) {
            sb.append(csvField(f.getRelativePath())).append(',').append(f.getSteps()).append(NEWLINE);
        }
        return sb.toString();
    }

    public String buildDiffCsv(DiffResult result) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("ファイルパス,全量(A),全量(B),追加,削除").append(NEWLINE);
        for (DiffFileResult f : result.getFiles()) {
            sb.append(csvField(f.getRelativePath())).append(',')
                    .append(f.getStepsA()).append(',')
                    .append(f.getStepsB()).append(',')
                    .append(f.getAdded()).append(',')
                    .append(f.getDeleted()).append(NEWLINE);
        }
        return sb.toString();
    }

    public void write(String csv, Path outFile) {
        try {
            Files.createDirectories(outFile.toAbsolutePath().getParent());
            Files.writeString(outFile, csv, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV to " + outFile, e);
        }
    }

    /** Quotes a field if it contains a comma, quote, or newline, doubling any embedded quotes. */
    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
