package com.stepcounter.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Appends run/export failures to a daily log file under ./logs so a user can hand over
 * diagnostics without having to reproduce the crash. Never throws — a logging failure must not
 * take down the app on top of whatever error triggered it.
 */
public final class ErrorLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ErrorLogger() {
    }

    /** Returns the log file path that {@link #log} would write to right now, for display in an alert. */
    public static Path currentLogFile() {
        return Path.of("logs", "step-counter-" + LocalDate.now() + ".log");
    }

    public static void log(String context, Throwable error) {
        Path logFile = currentLogFile();
        try {
            Files.createDirectories(logFile.toAbsolutePath().getParent());
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));

            String entry = "[" + LocalDateTime.now().format(TIMESTAMP) + "] " + context
                    + " failed: " + error.getMessage() + System.lineSeparator()
                    + trace + System.lineSeparator();

            Files.writeString(logFile, entry,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging is best-effort; don't let a logging failure compound the original error.
        }
    }
}
