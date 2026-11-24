package com.stepcounter.model;

public class FileStepResult {
    private final String relativePath;
    private final long steps;
    private final CommitInfo lastCommit;

    public FileStepResult(String relativePath, long steps) {
        this(relativePath, steps, CommitInfo.NONE);
    }

    public FileStepResult(String relativePath, long steps, CommitInfo lastCommit) {
        this.relativePath = relativePath;
        this.steps = steps;
        this.lastCommit = lastCommit == null ? CommitInfo.NONE : lastCommit;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSteps() {
        return steps;
    }

    /** The last commit that touched this file on the scanned branch. Never null (blank fields if unknown). */
    public CommitInfo getLastCommit() {
        return lastCommit;
    }
}
