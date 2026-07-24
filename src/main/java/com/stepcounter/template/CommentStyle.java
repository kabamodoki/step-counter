package com.stepcounter.template;

import java.util.List;

/** Comment syntax used to strip comment-only lines when counting in SUBSTANTIVE mode. */
public class CommentStyle {

    /** No comment concept at all (e.g. XML metadata) — every non-blank line counts. */
    public static final CommentStyle NONE = new CommentStyle(List.of(), null, null);

    private final List<String> lineCommentPrefixes;
    private final String blockCommentStart;
    private final String blockCommentEnd;

    public CommentStyle(List<String> lineCommentPrefixes, String blockCommentStart, String blockCommentEnd) {
        this.lineCommentPrefixes = lineCommentPrefixes;
        this.blockCommentStart = blockCommentStart;
        this.blockCommentEnd = blockCommentEnd;
    }

    public List<String> getLineCommentPrefixes() {
        return lineCommentPrefixes;
    }

    public String getBlockCommentStart() {
        return blockCommentStart;
    }

    public String getBlockCommentEnd() {
        return blockCommentEnd;
    }

    public boolean hasBlockComments() {
        return blockCommentStart != null && blockCommentEnd != null;
    }

    /** Line comments and block comments, C-style — Java, TypeScript, Apex classes/triggers, LWC/Aura JS. */
    public static CommentStyle cStyle() {
        return new CommentStyle(List.of("//"), "/*", "*/");
    }
}
