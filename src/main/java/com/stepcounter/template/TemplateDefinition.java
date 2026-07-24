package com.stepcounter.template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-template defaults: which folders/files to exclude, and how to recognize comment lines per extension. */
public class TemplateDefinition {

    /** Applied on top of every template's own excludes: CI config, docs, dotfiles, and static
     *  image assets are never "steps", regardless of which template is selected. */
    private static final List<String> COMMON_EXCLUDES = List.of(
            ".github/", "README*", "LICENSE*", ".*",
            "*.svg", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.ico", "*.webp", "*.bmp");

    private final TemplateType type;
    private final List<String> defaultExcludes;
    private final List<String> optionalExcludes;
    private final String optionalExcludeLabel;
    private final Map<String, CommentStyle> commentStyleByExtension;
    private final CommentStyle fallbackStyle;

    private TemplateDefinition(TemplateType type, List<String> templateExcludes,
                                List<String> optionalExcludes, String optionalExcludeLabel,
                                Map<String, CommentStyle> commentStyleByExtension,
                                CommentStyle fallbackStyle) {
        this.type = type;
        List<String> merged = new ArrayList<>(COMMON_EXCLUDES);
        merged.addAll(templateExcludes);
        merged.addAll(optionalExcludes);
        this.defaultExcludes = List.copyOf(merged);
        this.optionalExcludes = optionalExcludes;
        this.optionalExcludeLabel = optionalExcludeLabel;
        this.commentStyleByExtension = commentStyleByExtension;
        this.fallbackStyle = fallbackStyle;
    }

    public TemplateType getType() {
        return type;
    }

    /** Excludes applied when the template is first selected (baseline + any optional excludes, since those default to "excluded"). */
    public List<String> getDefaultExcludes() {
        return defaultExcludes;
    }

    /** Patterns toggled by this template's "include X" checkbox, if any (empty when the template has no such toggle). */
    public List<String> getOptionalExcludes() {
        return optionalExcludes;
    }

    /** Label for the optional-excludes checkbox, or {@code null} if this template doesn't have one. */
    public String getOptionalExcludeLabel() {
        return optionalExcludeLabel;
    }

    public CommentStyle styleFor(String fileName) {
        String lower = fileName.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot + 1) : "";
        return commentStyleByExtension.getOrDefault(ext, fallbackStyle);
    }

    public static TemplateDefinition java() {
        Map<String, CommentStyle> byExt = new LinkedHashMap<>();
        byExt.put("java", CommentStyle.cStyle());
        return new TemplateDefinition(
                TemplateType.JAVA,
                List.of("target/", "src/test/", "src/main/resources/", ".git/", "*.class",
                        "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
                        "mvnw", "mvnw.cmd", "gradlew", "gradlew.bat", ".mvn/", "gradle/", "lombok.config"),
                List.of(), null,
                byExt,
                CommentStyle.cStyle());
    }

    public static TemplateDefinition salesforce() {
        Map<String, CommentStyle> byExt = new LinkedHashMap<>();
        byExt.put("cls", CommentStyle.cStyle());
        byExt.put("trigger", CommentStyle.cStyle());
        byExt.put("js", CommentStyle.cStyle());
        byExt.put("xml", CommentStyle.NONE);
        return new TemplateDefinition(
                TemplateType.SALESFORCE,
                List.of(".sfdx/", "**/staticresources/", "**/lwc/**/__tests__/", ".git/", "*Test.cls"),
                List.of("**/profiles/", "**/permissionsets/", "**/permissionsetgroups/"),
                "プロファイル/パーミッション系を含める",
                byExt,
                CommentStyle.NONE);
    }

    public static TemplateDefinition typescript() {
        Map<String, CommentStyle> byExt = new LinkedHashMap<>();
        byExt.put("ts", CommentStyle.cStyle());
        byExt.put("tsx", CommentStyle.cStyle());
        byExt.put("js", CommentStyle.cStyle());
        byExt.put("jsx", CommentStyle.cStyle());
        return new TemplateDefinition(
                TemplateType.TYPESCRIPT,
                List.of("node_modules/", "dist/", "build/", "coverage/", ".git/", "*.d.ts", "**/__tests__/",
                        "*.spec.ts", "*.spec.tsx", "*.spec.js", "*.spec.jsx",
                        "*.test.ts", "*.test.tsx", "*.test.js", "*.test.jsx"),
                List.of(), null,
                byExt,
                CommentStyle.cStyle());
    }

    public static TemplateDefinition python() {
        Map<String, CommentStyle> byExt = new LinkedHashMap<>();
        byExt.put("py", CommentStyle.hashStyle());
        return new TemplateDefinition(
                TemplateType.PYTHON,
                List.of("__pycache__/", ".venv/", "venv/", "env/", ".pytest_cache/", ".mypy_cache/", ".tox/",
                        "dist/", "build/", "*.egg-info/", "*.pyc",
                        "pyproject.toml", "setup.py", "setup.cfg", "requirements*.txt",
                        "Pipfile", "Pipfile.lock", "poetry.lock",
                        "tests/", "test_*.py", "*_test.py"),
                List.of(), null,
                byExt,
                CommentStyle.hashStyle());
    }

    public static TemplateDefinition kotlin() {
        Map<String, CommentStyle> byExt = new LinkedHashMap<>();
        byExt.put("kt", CommentStyle.cStyle());
        byExt.put("kts", CommentStyle.cStyle());
        return new TemplateDefinition(
                TemplateType.KOTLIN,
                List.of("build/", ".gradle/", "gradle/", "gradlew", "gradlew.bat",
                        "settings.gradle.kts", "build.gradle.kts", "src/test/", "*Test.kt"),
                List.of(), null,
                byExt,
                CommentStyle.cStyle());
    }

    public static TemplateDefinition swift() {
        Map<String, CommentStyle> byExt = new LinkedHashMap<>();
        byExt.put("swift", CommentStyle.cStyle());
        return new TemplateDefinition(
                TemplateType.SWIFT,
                List.of(".build/", "DerivedData/", "*.xcodeproj/", "*.xcworkspace/", "Pods/",
                        "Package.resolved", "Package.swift", "Tests/", "*Tests.swift"),
                List.of(), null,
                byExt,
                CommentStyle.cStyle());
    }

    public static TemplateDefinition forType(TemplateType type) {
        return switch (type) {
            case JAVA -> java();
            case SALESFORCE -> salesforce();
            case TYPESCRIPT -> typescript();
            case PYTHON -> python();
            case KOTLIN -> kotlin();
            case SWIFT -> swift();
        };
    }
}
