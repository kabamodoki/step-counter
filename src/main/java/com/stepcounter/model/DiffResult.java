package com.stepcounter.model;

import java.util.Collections;
import java.util.List;

public class DiffResult {
    private final String repoUrl;
    private final String branchA;
    private final String branchB;
    private final String templateName;
    private final StepMode mode;
    private final List<String> excludePatterns;
    private final List<DiffFileResult> files;

    public DiffResult(String repoUrl, String branchA, String branchB, String templateName,
                       StepMode mode, List<String> excludePatterns, List<DiffFileResult> files) {
        this.repoUrl = repoUrl;
        this.branchA = branchA;
        this.branchB = branchB;
        this.templateName = templateName;
        this.mode = mode;
        this.excludePatterns = excludePatterns;
        this.files = files;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getBranchA() {
        return branchA;
    }

    public String getBranchB() {
        return branchB;
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

    public List<DiffFileResult> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public int getTotalAdded() {
        return files.stream().mapToInt(DiffFileResult::getAdded).sum();
    }

    public int getTotalDeleted() {
        return files.stream().mapToInt(DiffFileResult::getDeleted).sum();
    }
}
