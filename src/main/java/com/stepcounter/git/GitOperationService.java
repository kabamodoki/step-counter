package com.stepcounter.git;

import com.stepcounter.model.CommitInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Shells out to the locally installed `git` executable to fetch a single branch into a work directory. */
public class GitOperationService {

    private static final int TIMEOUT_MINUTES = 15;

    /** Marks a commit header line in our git log output (ASCII record separator; never appears in a path). */
    private static final String COMMIT_MARK = "\u001e";
    /** Separates author / date / subject within a header line (ASCII unit separator). */
    private static final String FIELD_SEP = "\u001f";

    /**
     * Clones {@code repoUrl} at {@code branch} into a fresh subdirectory of {@code parentWorkDir}
     * and returns the checkout root. Assumes the caller's environment already has git
     * authentication configured (credential helper / SSH agent) — this tool does not handle auth.
     * <p>
     * Uses a blobless clone: full commit/tree history (needed for {@link #lastCommits}) but only the
     * file contents the checkout needs. Servers without partial-clone support fall back to a normal clone.
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
                "git", "clone", "--branch", branch, "--single-branch", "--filter=blob:none",
                repoUrl, target.toString());
        return target;
    }

    /**
     * Finds, for each of {@code paths} (repo-relative, '/'-separated), the newest commit on the checked-out
     * branch that touched it. Does a single {@code git log --name-only} pass, newest first, and stops reading
     * once every path has been found — far cheaper than one {@code git log -1} per file.
     * <p>
     * {@code --no-renames} keeps git from comparing file contents (which would trigger blob downloads in a
     * blobless clone); a renamed file's last commit is therefore the rename itself. Merge commits show no
     * file list by default, so a change made only while resolving a merge is attributed to the prior commit.
     * Paths with no matching commit are simply absent from the returned map.
     */
    public Map<String, CommitInfo> lastCommits(Path repoRoot, Collection<String> paths) {
        Set<String> remaining = new HashSet<>(paths);
        Map<String, CommitInfo> found = new HashMap<>();
        if (remaining.isEmpty()) {
            return found;
        }
        List<String> command = List.of(
                "git", "-c", "core.quotepath=false", "-c", "i18n.logOutputEncoding=UTF-8",
                "log", "--no-renames", "--name-only",
                "--date=format-local:%Y-%m-%d %H:%M:%S",
                // %x1e / %x1f are git's escapes for COMMIT_MARK / FIELD_SEP (keeps control chars off the command line)
                "--format=%x1e%an%x1f%ad%x1f%s");
        Process process;
        try {
            process = new ProcessBuilder(command).directory(repoRoot.toFile()).start();
        } catch (IOException e) {
            throw new GitOperationException("Could not run git — is it installed and on PATH?", e);
        }
        // Drain stderr on the side so a chatty git can't block on a full pipe while we read stdout.
        StringBuilder stderr = new StringBuilder();
        Thread stderrReader = new Thread(() -> {
            try {
                stderr.append(new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // only used for the error message
            }
        }, "git-log-stderr");
        stderrReader.setDaemon(true);
        stderrReader.start();

        boolean stoppedEarly = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            CommitInfo current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(COMMIT_MARK)) {
                    String[] parts = line.substring(COMMIT_MARK.length()).split(FIELD_SEP, 3);
                    current = new CommitInfo(
                            parts.length > 0 ? parts[0] : "",
                            parts.length > 1 ? parts[1] : "",
                            parts.length > 2 ? parts[2] : "");
                } else if (!line.isEmpty() && current != null && remaining.remove(line)) {
                    found.put(line, current);
                    if (remaining.isEmpty()) {
                        stoppedEarly = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            process.destroyForcibly();
            throw new GitOperationException("Failed to read git log output", e);
        }

        if (stoppedEarly) {
            process.destroyForcibly();
            return found;
        }
        try {
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new GitOperationException("git command timed out: " + String.join(" ", command));
            }
            stderrReader.join(TimeUnit.SECONDS.toMillis(5));
            if (process.exitValue() != 0) {
                throw new GitOperationException(
                        "git command failed (exit " + process.exitValue() + "): " + String.join(" ", command)
                                + System.lineSeparator() + stderr);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new GitOperationException("git command interrupted: " + String.join(" ", command), e);
        }
        return found;
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
