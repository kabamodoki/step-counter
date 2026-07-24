package com.stepcounter.model;

import java.util.Collections;
import java.util.List;

public class ScanResult {
    private final String repoUrl;
    private final String branch;
    private final String templateName;
    private final StepMode mode;
    private final List<String> excludePatterns;
    private final List<FileStepResult> files;
    private final long substantiveTotalSteps;

    public ScanResult(String repoUrl, String branch, String templateName, StepMode mode,
                       List<String> excludePatterns, List<FileStepResult> files,
                       long substantiveTotalSteps) {
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.templateName = templateName;
        this.mode = mode;
        this.excludePatterns = excludePatterns;
        this.files = files;
        this.substantiveTotalSteps = substantiveTotalSteps;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getBranch() {
        return branch;
    }

    public String getTemplateName() {
        return templateName;
    }

    public StepMode getMode() {
        return mode;
    }

    public List<String> getExcludePatterns() {
        return Collections.unmodifiableList(excludePatterns);
    }

    public List<FileStepResult> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public long getTotalSteps() {
        return files.stream().mapToLong(FileStepResult::getSteps).sum();
    }

    /** Total steps under the *other* step mode, shown as a reference chip in the UI/report. */
    public long getSubstantiveTotalSteps() {
        return substantiveTotalSteps;
    }

    public int getFileCount() {
        return files.size();
    }
}
