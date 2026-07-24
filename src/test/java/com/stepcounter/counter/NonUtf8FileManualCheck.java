package com.stepcounter.counter;

import com.stepcounter.model.StepMode;
import com.stepcounter.template.CommentStyle;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Manual driver (not JUnit) reproducing the "MalformedInputException: Input length = 1" bug
 * reported against a Shift-JIS(CP932) encoded .scss file scanned under the TypeScript template,
 * and confirming LineCounter no longer throws on it.
 */
public final class NonUtf8FileManualCheck {

    public static void main(String[] args) throws IOException {
        Path scss = Files.createTempFile("style-check", ".scss");
        String content = String.join("\n", List.of(
                ".button {",
                "  color: red; // コメント（Shift-JISで保存）",
                "}"
        ));
        Files.write(scss, content.getBytes(Charset.forName("Shift-JIS")));

        LineCounter counter = new LineCounter();
        long fullSteps = counter.count(scss, CommentStyle.cStyle(), StepMode.FULL);
        long substantiveSteps = counter.count(scss, CommentStyle.cStyle(), StepMode.SUBSTANTIVE);
        Files.deleteIfExists(scss);

        if (fullSteps != 3) {
            throw new AssertionError("expected 3 physical lines, got " + fullSteps);
        }
        if (substantiveSteps != 3) {
            // all 3 lines have real code content (line 2 has "color: red;" before the trailing comment)
            throw new AssertionError("expected 3 substantive lines, got " + substantiveSteps);
        }
        System.out.println("OK: no MalformedInputException, FULL=" + fullSteps + " SUBSTANTIVE=" + substantiveSteps);
    }
}
