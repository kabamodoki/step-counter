package com.stepcounter.model;

/** The last commit that touched a file: author name, author date (local time, formatted), and subject line. */
public record CommitInfo(String author, String date, String message) {

    /** Placeholder for a file whose last commit couldn't be found; all fields render as blank. */
    public static final CommitInfo NONE = new CommitInfo("", "", "");
}
