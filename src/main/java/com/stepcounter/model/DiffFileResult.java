package com.stepcounter.model;

public class DiffFileResult {
    private final String relativePath;
    private final int added;
    private final int deleted;

    public DiffFileResult(String relativePath, int added, int deleted) {
        this.relativePath = relativePath;
        this.added = added;
        this.deleted = deleted;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public int getAdded() {
        return added;
    }

    public int getDeleted() {
        return deleted;
    }

    public int getNetChange() {
        return added - deleted;
    }
}
