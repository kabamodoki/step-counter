package com.stepcounter.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Manual driver (not JUnit) verifying ErrorLogger actually writes a log entry to disk. */
public final class ErrorLoggerManualCheck {

    public static void main(String[] args) throws IOException {
        Path logFile = ErrorLogger.currentLogFile();
        Files.deleteIfExists(logFile);

        ErrorLogger.log("実行", new RuntimeException("boom: git clone failed"));
        ErrorLogger.log("エクスポート", new RuntimeException("boom: disk full"));

        if (!Files.exists(logFile)) {
            throw new AssertionError("log file was not created: " + logFile);
        }
        String content = Files.readString(logFile);
        if (!content.contains("実行 failed: boom: git clone failed") || !content.contains("エクスポート failed: boom: disk full")
                || !content.contains("RuntimeException")) {
            throw new AssertionError("log content missing expected entries:\n" + content);
        }
        System.out.println("OK: " + logFile.toAbsolutePath());
        System.out.println("---- content ----");
        System.out.println(content);
    }
}
