package com.stepcounter.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-template defaults: which folders/files to exclude, and how to recognize comment lines per extension. */
public class TemplateDefinition {

    private final TemplateType type;
    private final List<String> defaultExcludes;
    private final Map<String, CommentStyle> commentStyleByExtension;
    private final CommentStyle fallbackStyle;

    private TemplateDefinition(TemplateType type, List<String> defaultExcludes,
                                Map<String, CommentStyle> commentStyleByExtension,
                                CommentStyle fallbackStyle) {
        this.type = type;
        this.defaultExcludes = defaultExcludes;
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
                List.of("target/", "src/test/", "src/main/resources/", ".git/", "*.class"),
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
                List.of("node_modules/", "dist/", "build/", "coverage/", ".git/", "*.d.ts"),
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
