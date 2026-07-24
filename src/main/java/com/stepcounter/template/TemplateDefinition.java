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
    private final Map<String, CommentStyle> commentStyleByExtension;
    private final CommentStyle fallbackStyle;

    private TemplateDefinition(TemplateType type, List<String> templateExcludes,
                                Map<String, CommentStyle> commentStyleByExtension,
                                CommentStyle fallbackStyle) {
        this.type = type;
        List<String> merged = new ArrayList<>(COMMON_EXCLUDES);
        merged.addAll(templateExcludes);
        this.defaultExcludes = List.copyOf(merged);
        this.commentStyleByExtension = commentStyleByExtension;
        this.fallbackStyle = fallbackStyle;
    }

    public TemplateType getType() {
        return type;
    }

    public List<String> getDefaultExcludes() {
        return defaultExcludes;
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
                List.of(".sfdx/", "**/staticresources/", "**/lwc/**/__tests__/", ".git/"),
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
                byExt,
                CommentStyle.cStyle());
    }

    public static TemplateDefinition forType(TemplateType type) {
        return switch (type) {
            case JAVA -> java();
            case SALESFORCE -> salesforce();
            case TYPESCRIPT -> typescript();
        };
    }
}
