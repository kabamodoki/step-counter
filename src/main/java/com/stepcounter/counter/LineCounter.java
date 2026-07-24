package com.stepcounter.counter;

import com.stepcounter.model.StepMode;
import com.stepcounter.template.CommentStyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Counts steps in a single file, either as raw physical lines or with comments/blank lines stripped. */
public class LineCounter {

    public long count(Path file, CommentStyle style, StepMode mode) {
        return significantLines(file, style, mode).size();
    }

    /**
     * Returns the lines that count under the given mode. FULL mode returns every physical line
     * (used as-is for both counting and diffing). SUBSTANTIVE mode strips blank lines and
     * comment-only lines according to the template's comment syntax, which also makes it safe
     * to diff without comment-only churn showing up as changes.
     *
     * <p>Note: this is a line-oriented scan, not a real lexer — it does not know about string/char
     * literals, so a comment marker embedded in a string literal (e.g. {@code String s = "//";})
     * would be (mis)treated as starting a comment. Acceptable for step-counting purposes; a real
     * tokenizer would be needed to close that gap completely.
     */
    public List<String> significantLines(Path file, CommentStyle style, StepMode mode) {
        List<String> rawLines = readLinesLeniently(file);

        if (mode == StepMode.FULL) {
            return rawLines;
        }

        List<String> significant = new ArrayList<>();
        boolean inBlockComment = false;
        for (String line : rawLines) {
            LineScanResult scan = scanLine(line, style, inBlockComment);
            inBlockComment = scan.endsInBlockComment;
            if (scan.hasCode) {
                significant.add(line);
            }
        }
        return significant;
    }

    private record LineScanResult(boolean hasCode, boolean endsInBlockComment) {
    }

    /**
     * Walks a single line left to right, alternating between "in code" and "in block comment"
     * segments, so that e.g. {@code /* note *}{@code / doStuff();} counts as code (trailing code
     * after the comment closes) and {@code doStuff(); /* trailing *}{@code /} also counts as code
     * (leading code before the comment opens).
     */
    private LineScanResult scanLine(String line, CommentStyle style, boolean startsInBlockComment) {
        boolean inBlock = startsInBlockComment;
        boolean hasCode = false;
        int pos = 0;
        int length = line.length();

        while (pos < length) {
            if (inBlock) {
                int end = style.hasBlockComments() ? line.indexOf(style.getBlockCommentEnd(), pos) : -1;
                if (end < 0) {
                    return new LineScanResult(hasCode, true);
                }
                pos = end + style.getBlockCommentEnd().length();
                inBlock = false;
                continue;
            }

            int lineCommentIdx = -1;
            for (String prefix : style.getLineCommentPrefixes()) {
                int idx = line.indexOf(prefix, pos);
                if (idx >= 0 && (lineCommentIdx < 0 || idx < lineCommentIdx)) {
                    lineCommentIdx = idx;
                }
            }
            int blockCommentIdx = style.hasBlockComments() ? line.indexOf(style.getBlockCommentStart(), pos) : -1;

            int nextMarker;
            boolean nextIsBlock;
            if (lineCommentIdx < 0 && blockCommentIdx < 0) {
                nextMarker = -1;
                nextIsBlock = false;
            } else if (lineCommentIdx < 0) {
                nextMarker = blockCommentIdx;
                nextIsBlock = true;
            } else if (blockCommentIdx < 0) {
                nextMarker = lineCommentIdx;
                nextIsBlock = false;
            } else if (blockCommentIdx < lineCommentIdx) {
                nextMarker = blockCommentIdx;
                nextIsBlock = true;
            } else {
                nextMarker = lineCommentIdx;
                nextIsBlock = false;
            }

            if (nextMarker < 0) {
                if (!line.substring(pos).isBlank()) {
                    hasCode = true;
                }
                pos = length;
            } else {
                if (!line.substring(pos, nextMarker).isBlank()) {
                    hasCode = true;
                }
                if (nextIsBlock) {
                    pos = nextMarker + style.getBlockCommentStart().length();
                    inBlock = true;
                } else {
                    // rest of the line is a line comment
                    pos = length;
                }
            }
        }
        return new LineScanResult(hasCode, inBlock);
    }

    /**
     * Reads a file as UTF-8, replacing rather than throwing on malformed/unmappable byte
     * sequences. Real-world repos routinely mix in files saved with another encoding
     * (Shift-JIS/CP932 is common for older SCSS/CSS/properties files); a strict UTF-8 decode
     * would blow up the whole scan with a {@code MalformedInputException} over a single bad file.
     */
    private List<String> readLinesLeniently(Path file) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file), decoder))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + file, e);
        }
        return lines;
    }
}
