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
                Map.entry("src/test/java/com/example/AppTest.java", true),
                Map.entry("LICENSE", true),
                Map.entry("LICENSE.md", true),
                Map.entry("src/main/resources/logo.png", true),
                Map.entry("src/main/resources/icon.svg", true)
        );

        boolean allOk = checkAll("Java", java.getDefaultExcludes(), cases);

        TemplateDefinition ts = TemplateDefinition.typescript();
        Map<String, Boolean> tsCases = Map.ofEntries(
                Map.entry("src/components/__tests__/Button.test.ts", true),
                Map.entry("src/__tests__/setup.ts", true),
                Map.entry("src/components/Button.tsx", false),
                Map.entry("public/logo.png", true),
                Map.entry("public/favicon.ico", true),
                Map.entry("LICENSE", true)
        );
        allOk &= checkAll("TypeScript", ts.getDefaultExcludes(), tsCases);

        TemplateDefinition sf = TemplateDefinition.salesforce();
        Map<String, Boolean> sfCases = Map.ofEntries(
                Map.entry("force-app/main/default/staticresources/logo.resource", true),
                Map.entry("force-app/main/default/lwc/myComponent/__tests__/myComponent.test.js", true),
                Map.entry("force-app/main/default/lwc/myComponent/myComponent.js", false),
                Map.entry("force-app/main/default/classes/MyClass.cls", false)
        );
        allOk &= checkAll("Salesforce", sf.getDefaultExcludes(), sfCases);

        System.out.println(allOk ? "ALL OK" : "SOME MISMATCHES");
        if (!allOk) {
            System.exit(1);
        }
    }

    private static boolean checkAll(String label, List<String> excludes, Map<String, Boolean> cases) {
        boolean allOk = true;
        for (var entry : cases.entrySet()) {
            boolean actual = ExcludeMatcher.isExcluded(Path.of(entry.getKey()), excludes);
            boolean expected = entry.getValue();
            String status = actual == expected ? "OK  " : "FAIL";
            if (actual != expected) {
                allOk = false;
            }
            System.out.printf("[%s] %s  %-55s expected=%-5s actual=%-5s%n", label, status, entry.getKey(), expected, actual);
        }
        return allOk;
    }
}
