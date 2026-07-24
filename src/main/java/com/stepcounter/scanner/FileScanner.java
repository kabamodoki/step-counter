package com.stepcounter.scanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Walks a checked-out repo root and returns the files that survive the exclude patterns. */
public class FileScanner {

    public List<Path> scan(Path repoRoot, List<String> excludePatterns) {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(repoRoot)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> {
                        Path relative = repoRoot.relativize(path);
                        if (!ExcludeMatcher.isExcluded(relative, excludePatterns)) {
                            result.add(relative);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan " + repoRoot, e);
        }
        result.sort(Comparator.comparing(Path::toString));
        return result;
    }
}
