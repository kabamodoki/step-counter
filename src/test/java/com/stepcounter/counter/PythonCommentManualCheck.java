package com.stepcounter.counter;

import com.stepcounter.model.StepMode;
import com.stepcounter.template.CommentStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Manual driver (not JUnit) verifying Python's hash-style comment handling in LineCounter. */
public final class PythonCommentManualCheck {

    public static void main(String[] args) throws IOException {
        Path tmp = Files.createTempFile("python-check", ".py");
        Files.writeString(tmp, String.join("\n", List.of(
                "import os",                       // 1 code
                "",                                  // 2 blank
                "# a pure comment line",             // 3 comment-only
                "x = 1  # trailing comment",         // 4 code
                "\"\"\"a module docstring\"\"\"",     // 5 code (docstring intentionally NOT treated as comment)
                "def main():",                       // 6 code
                "    return x"                        // 7 code
        )));

        LineCounter counter = new LineCounter();
        List<String> significant = counter.significantLines(tmp, CommentStyle.hashStyle(), StepMode.SUBSTANTIVE);
        Files.deleteIfExists(tmp);

        List<String> expected = List.of(
                "import os",
                "x = 1  # trailing comment",
                "\"\"\"a module docstring\"\"\"",
                "def main():",
                "    return x"
        );

        if (significant.equals(expected)) {
            System.out.println("OK: " + significant.size() + " substantive lines as expected");
        } else {
            System.out.println("MISMATCH");
            System.out.println("expected: " + expected);
            System.out.println("actual:   " + significant);
            System.exit(1);
        }
    }
}
