package com.stepcounter.model;

public class DiffFileResult {
    private final String relativePath;
    private final long stepsA;
    private final long stepsB;
    private final int added;
    private final int deleted;

    public DiffFileResult(String relativePath, long stepsA, long stepsB, int added, int deleted) {
        this.relativePath = relativePath;
        this.stepsA = stepsA;
        this.stepsB = stepsB;
        this.added = added;
        this.deleted = deleted;
    }

    public String getRelativePath() {
        return relativePath;
    }

    /** Total steps for this file on branch A (the "from" branch), under the run's step mode. 0 if the file doesn't exist on A. */
    public long getStepsA() {
        return stepsA;
    }

    /** Total steps for this file on branch B (the "to" branch), under the run's step mode. 0 if the file doesn't exist on B. */
    public long getStepsB() {
        return stepsB;
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
