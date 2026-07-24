package com.stepcounter.git;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Shells out to the locally installed `git` executable to fetch a single branch into a work directory. */
public class GitOperationService {

    private static final int TIMEOUT_MINUTES = 15;

    /**
     * Clones {@code repoUrl} at {@code branch} into a fresh subdirectory of {@code parentWorkDir}
     * and returns the checkout root. Assumes the caller's environment already has git
     * authentication configured (credential helper / SSH agent) — this tool does not handle auth.
     */
    public Path fetchBranch(String repoUrl, String branch, Path parentWorkDir, String label) {
        try {
            Files.createDirectories(parentWorkDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Path target = parentWorkDir.resolve(label);
        cleanup(target);

        run(parentWorkDir,
                "git", "clone", "--branch", branch, "--single-branch", "--depth", "1",
                repoUrl, target.toString());
        return target;
    }

    /** Recursively deletes a work directory (e.g. after a run finishes). Safe to call on a missing path. */
    public void cleanup(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup of a temp checkout
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void run(Path workingDir, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new GitOperationException("git command timed out: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                throw new GitOperationException(
                        "git command failed (exit " + process.exitValue() + "): " + String.join(" ", command)
                                + System.lineSeparator() + output);
            }
        } catch (IOException e) {
            throw new GitOperationException("Could not run git — is it installed and on PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitOperationException("git command interrupted: " + String.join(" ", command), e);
        }
    }
}
