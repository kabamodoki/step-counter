package com.stepcounter.model;

public class DiffFileResult {
    private final String relativePath;
    private final long stepsA;
    private final long stepsB;
    private final int added;
    private final int deleted;
    private final CommitInfo lastCommit;

    public DiffFileResult(String relativePath, long stepsA, long stepsB, int added, int deleted) {
        this(relativePath, stepsA, stepsB, added, deleted, CommitInfo.NONE);
    }

    public DiffFileResult(String relativePath, long stepsA, long stepsB, int added, int deleted, CommitInfo lastCommit) {
        this.relativePath = relativePath;
        this.stepsA = stepsA;
        this.stepsB = stepsB;
        this.added = added;
        this.deleted = deleted;
        this.lastCommit = lastCommit == null ? CommitInfo.NONE : lastCommit;
    }

    /** Returns a copy of this result carrying the given last-commit info. */
    public DiffFileResult withLastCommit(CommitInfo commit) {
        return new DiffFileResult(relativePath, stepsA, stepsB, added, deleted, commit);
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

    /** The last commit that touched this file (branch B's history, falling back to A's). Never null. */
    public CommitInfo getLastCommit() {
        return lastCommit;
    }
}
