package com.stepcounter.scanner;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 * Matches a repo-relative path against user/template exclude patterns.
 * Supported pattern forms:
 *  - "target/"          -> directory prefix, matches that folder anywhere in the tree
 *  - "*.class"           -> glob against the file name
 *  - "**{@literal /}lwc/**{@literal /}__tests__/" -> glob against the full relative path
 *  - "README.md"         -> exact file name match anywhere in the tree
 */
public final class ExcludeMatcher {

    private ExcludeMatcher() {
    }

    public static boolean isExcluded(Path relativePath, List<String> patterns) {
        String normalized = relativePath.toString().replace('\\', '/');
        String fileName = relativePath.getFileName().toString();

        for (String raw : patterns) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.endsWith("/")) {
                String dir = pattern.substring(0, pattern.length() - 1);
                if (normalized.equals(dir) || normalized.startsWith(dir + "/") || normalized.contains("/" + dir + "/")) {
                    return true;
                }
                continue;
            }
            if (pattern.contains("*") || pattern.contains("?")) {
                if (pattern.contains("/")) {
                    if (globMatches(normalized, pattern)) {
                        return true;
                    }
                } else if (globMatches(fileName, pattern)) {
                    return true;
                }
                continue;
            }
            if (normalized.equals(pattern) || normalized.endsWith("/" + pattern) || fileName.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean globMatches(String candidate, String globPattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
        return matcher.matches(Path.of(candidate));
    }
}
