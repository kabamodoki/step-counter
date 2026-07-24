package com.stepcounter.template;

import com.stepcounter.scanner.ExcludeMatcher;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Manual driver (not JUnit) verifying the new common excludes (.github/, README*, dotfiles) and
 *  the Java template's build-tool/config excludes actually match as expected. */
public final class CommonExcludeManualCheck {

    public static void main(String[] args) {
        TemplateDefinition java = TemplateDefinition.java();
        List<String> excludes = java.getDefaultExcludes();

        Map<String, Boolean> cases = Map.ofEntries(
                Map.entry(".github/workflows/ci.yml", true),
                Map.entry("README.md", true),
                // Windows path matching is case-insensitive, so README* also catches this — desired,
                // since "readme.txt" is the same kind of doc file regardless of case.
                Map.entry("readme.txt", true),
                Map.entry(".gitignore", true),
                Map.entry(".gitkeep", true),
                Map.entry(".env", true),
                Map.entry("pom.xml", true),
                Map.entry("build.gradle", true),
                Map.entry("build.gradle.kts", true),
                Map.entry("mvnw", true),
                Map.entry("mvnw.cmd", true),
                Map.entry("gradlew", true),
                Map.entry(".mvn/wrapper/maven-wrapper.properties", true),
                Map.entry("gradle/wrapper/gradle-wrapper.properties", true),
                Map.entry("lombok.config", true),
                Map.entry("src/main/java/com/example/App.java", false),
                Map.entry("src/main/resources/application.yml", true),
                Map.entry("src/test/java/com/example/AppTest.java", true)
        );

        boolean allOk = true;
        for (var entry : cases.entrySet()) {
            boolean actual = ExcludeMatcher.isExcluded(Path.of(entry.getKey()), excludes);
            boolean expected = entry.getValue();
            String status = actual == expected ? "OK  " : "FAIL";
            if (actual != expected) {
                allOk = false;
            }
            System.out.printf("%s  %-55s expected=%-5s actual=%-5s%n", status, entry.getKey(), expected, actual);
        }
        System.out.println(allOk ? "ALL OK" : "SOME MISMATCHES");
        if (!allOk) {
            System.exit(1);
        }
    }
}
