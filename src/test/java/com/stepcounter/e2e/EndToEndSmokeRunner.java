package com.stepcounter.e2e;

import com.stepcounter.diff.DiffCounter;
import com.stepcounter.git.GitOperationService;
import com.stepcounter.model.DiffFileResult;
import com.stepcounter.model.StepMode;
import com.stepcounter.scanner.FileScanner;
import com.stepcounter.template.TemplateDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Manual end-to-end driver (not JUnit): runs the real GitOperationService against an actual
 * local git repository (this project's own repo, addressed by file path) to exercise
 * clone -> scan -> count -> diff without any external network dependency.
 *
 * Usage: java ... EndToEndSmokeRunner <path-to-repo> <branchA> <branchB>
 */
public final class EndToEndSmokeRunner {

    public static void main(String[] args) throws Exception {
        String repoPath = args[0];
        String branchA = args[1];
        String branchB = args[2];

        GitOperationService git = new GitOperationService();
        FileScanner scanner = new FileScanner();
        DiffCounter diffCounter = new DiffCounter();
        TemplateDefinition template = TemplateDefinition.java();

        Path workDir = Path.of(System.getProperty("java.io.tmpdir"), "step-counter-e2e", UUID.randomUUID().toString());

        System.out.println("== fetching " + branchA + " ==");
        Path rootA = git.fetchBranch(repoPath, branchA, workDir, "a");
        List<Path> filesA = scanner.scan(rootA, template.getDefaultExcludes());
        System.out.println(branchA + ": " + filesA.size() + " files after exclude filtering");
        filesA.forEach(f -> System.out.println("  " + f));

        System.out.println("== fetching " + branchB + " ==");
        Path rootB = git.fetchBranch(repoPath, branchB, workDir, "b");
        List<Path> filesB = scanner.scan(rootB, template.getDefaultExcludes());
        System.out.println(branchB + ": " + filesB.size() + " files after exclude filtering");

        System.out.println("== diff (FULL mode) ==");
        List<DiffFileResult> diffsFull = diffCounter.compare(rootA, rootB, template, template.getDefaultExcludes(), StepMode.FULL);
        diffsFull.forEach(d -> System.out.printf("  %s  +%d -%d%n", d.getRelativePath(), d.getAdded(), d.getDeleted()));

        System.out.println("== diff (SUBSTANTIVE mode) ==");
        List<DiffFileResult> diffsSubstantive = diffCounter.compare(rootA, rootB, template, template.getDefaultExcludes(), StepMode.SUBSTANTIVE);
        diffsSubstantive.forEach(d -> System.out.printf("  %s  +%d -%d%n", d.getRelativePath(), d.getAdded(), d.getDeleted()));

        git.cleanup(workDir);
        System.out.println("== cleanup done ==");
    }
}
