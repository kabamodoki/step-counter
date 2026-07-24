package com.stepcounter.counter;

import com.stepcounter.model.StepMode;
import com.stepcounter.template.CommentStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Manual driver (not JUnit) exercising LineCounter's inline-comment edge cases: comments that
 * open, close, or share a line with real code. Not part of the automated build.
 */
public final class LineCounterManualCheck {

    public static void main(String[] args) throws IOException {
        Path tmp = Files.createTempFile("line-counter-check", ".java");
        Files.writeString(tmp, String.join("\n", List.of(
                "package demo;",                          // 1 code
                "",                                        // 2 blank
                "// pure line comment",                    // 3 comment-only
                "int x = 1; // trailing comment",          // 4 code (has trailing comment)
                "/* pure block comment */",                // 5 comment-only (opens+closes)
                "/* leading */ int y = 2;",                 // 6 code (leading block comment, trailing code)
                "int z = 3; /* trailing block */",          // 7 code (trailing block comment)
                "/* multi",                                 // 8 comment-only (opens, doesn't close)
                "   line",                                  // 9 comment-only (still inside block)
                "   comment */ int w = 4;",                 // 10 code (block closes, trailing code)
                "int done = 5;"                             // 11 code
        )));

        LineCounter counter = new LineCounter();
        List<String> significant = counter.significantLines(tmp, CommentStyle.cStyle(), StepMode.SUBSTANTIVE);
        Files.deleteIfExists(tmp);

        List<String> expected = List.of(
                "package demo;",
                "int x = 1; // trailing comment",
                "/* leading */ int y = 2;",
                "int z = 3; /* trailing block */",
                "   comment */ int w = 4;",
                "int done = 5;"
        );

        if (significant.equals(expected)) {
            System.out.println("OK: significantLines matched expected " + expected.size() + " code lines");
        } else {
            System.out.println("MISMATCH");
            System.out.println("expected: " + expected);
            System.out.println("actual:   " + significant);
            System.exit(1);
        }
    }
}
