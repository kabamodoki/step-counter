package com.stepcounter.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.stepcounter.counter.LineCounter;
import com.stepcounter.model.DiffFileResult;
import com.stepcounter.model.StepMode;
import com.stepcounter.scanner.ExcludeMatcher;
import com.stepcounter.scanner.FileScanner;
import com.stepcounter.template.TemplateDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares two branch checkouts file-by-file. Diffing runs on the same "significant lines" the
 * step counter uses, so in SUBSTANTIVE mode a comment-only edit doesn't show up as an added/removed step.
 */
public class DiffCounter {

    private final FileScanner fileScanner = new FileScanner();
    private final LineCounter lineCounter = new LineCounter();

    public List<DiffFileResult> compare(Path rootA, Path rootB, TemplateDefinition template,
                                         List<String> excludePatterns, StepMode mode) {
        Set<Path> files = new LinkedHashSet<>();
        files.addAll(fileScanner.scan(rootA, excludePatterns));
        files.addAll(fileScanner.scan(rootB, excludePatterns));

        List<DiffFileResult> results = new ArrayList<>();
        for (Path relative : files) {
            if (ExcludeMatcher.isExcluded(relative, excludePatterns)) {
                continue;
            }
            Path fileA = rootA.resolve(relative);
            Path fileB = rootB.resolve(relative);
            boolean existsA = Files.isRegularFile(fileA);
            boolean existsB = Files.isRegularFile(fileB);
            if (!existsA && !existsB) {
                continue;
            }

            var style = template.styleFor(relative.getFileName().toString());
            List<String> linesA = existsA ? lineCounter.significantLines(fileA, style, mode) : List.of();
            List<String> linesB = existsB ? lineCounter.significantLines(fileB, style, mode) : List.of();

            Patch<String> patch = DiffUtils.diff(linesA, linesB);
            int added = 0;
            int deleted = 0;
            for (AbstractDelta<String> delta : patch.getDeltas()) {
                added += delta.getTarget().size();
                deleted += delta.getSource().size();
            }
            if (added > 0 || deleted > 0) {
                results.add(new DiffFileResult(relative.toString().replace('\\', '/'), added, deleted));
            }
        }
        results.sort((a, b) -> Integer.compare(b.getAdded() + b.getDeleted(), a.getAdded() + a.getDeleted()));
        return results;
    }
}
