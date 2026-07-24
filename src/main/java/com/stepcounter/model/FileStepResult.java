package com.stepcounter.model;

public class FileStepResult {
    private final String relativePath;
    private final long steps;

    public FileStepResult(String relativePath, long steps) {
        this.relativePath = relativePath;
        this.steps = steps;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSteps() {
        return steps;
    }
}
