
package com.leetsync.backend.service;

import com.leetsync.backend.dto.*;
import com.leetsync.backend.exception.ApiException;
import com.leetsync.backend.github.GitHubClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubSyncService {

    private final GitHubClient githubClient;
    private final CodeFileService codeFileService;
    private final SubmissionFormatter formatter;
    private final GitHubPathService legacyPathService;

    @Autowired
    public GitHubSyncService(
            GitHubClient githubClient,
            CodeFileService codeFileService,
            SubmissionFormatter formatter,
            GitHubPathService legacyPathService
    ) {
        this.githubClient = githubClient;
        this.codeFileService = codeFileService;
        this.formatter = formatter;
        this.legacyPathService = legacyPathService;
    }

    public GitHubSyncService(
            GitHubClient githubClient,
            CodeFileService codeFileService,
            SubmissionFormatter formatter
    ) {
        this(
                githubClient,
                codeFileService,
                formatter,
                new GitHubPathService()
        );
    }

    public SyncResponse sync(
            String token,
            String owner,
            SubmissionRequest request
    ) {
        if (!Boolean.TRUE.equals(request.accepted())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NOT_ACCEPTED",
                    "Only confirmed accepted submissions can be synchronized."
            );
        }

        if (request.repository() == null
                || request.repository().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MISSING_REPOSITORY",
                    "repository is required."
            );
        }

        String repo = request.repository();

        GitHubRepositoryResponse repository =
                githubClient.getRepository(token, owner, repo);

        String branch = request.branch() == null
                || request.branch().isBlank()
                ? repository.defaultBranch()
                : request.branch();

        String solutionPath = codeFileService.path(request);
        String problemReadmePath =
                codeFileService.problemReadmePath(request);

        GitHubContentsResponse existingSolution =
                githubClient.getFile(
                        token, owner, repo, solutionPath, branch
                );

        boolean solutionExists =
                existingSolution != null
                        && existingSolution.content() != null;

        // Never create a duplicate of an existing legacy LeetCode solution.
        if (!solutionExists) {
            String legacySolutionPath = getLegacySolutionPath(request);

            if (legacySolutionPath != null
                    && !legacySolutionPath.equals(solutionPath)) {
                GitHubContentsResponse legacySolution =
                        githubClient.getFile(
                                token, owner, repo,
                                legacySolutionPath, branch
                        );

                if (legacySolution != null
                        && legacySolution.content() != null) {
                    return SyncResponse.skipped(
                            "A solution already exists at the legacy path: "
                                    + legacySolutionPath
                                    + ". No duplicate file was created. "
                                    + "The existing file was left unchanged.",
                            legacySolutionPath
                    );
                }
            }
        }

        GitHubContentsResponse existingProblemReadme =
                githubClient.getFile(
                        token, owner, repo,
                        problemReadmePath, branch
                );

        boolean problemReadmeExists =
                existingProblemReadme != null
                        && existingProblemReadme.content() != null;

        String pendingMarker = pendingProblemMarker(request);

        // This marker survives a partial failure and makes retry recovery possible.
        boolean pendingCount = problemReadmeExists
                && decode(existingProblemReadme.content())
                .contains(pendingMarker);

        boolean isNewProblem =
                !solutionExists && !problemReadmeExists;

        boolean shouldCountProblem = isNewProblem || pendingCount;

        GitHubContentsResponse verifiedRootReadme = null;

        // Verify the root README before making any writes for a new
        // problem or resuming an interrupted global-stat update.
        if (shouldCountProblem) {
            verifiedRootReadme = verifyRootReadme(
                    token, owner, repo, branch
            );
        }

        String formattedCode = formatter.format(request);

        // Create a missing solution.
        if (!solutionExists) {
            String message = buildCommitMessage(request, false);

            if (shouldCountProblem && !pendingCount) {
                String readmeContent =
                        "# " + request.problemId()
                                + ". " + request.problemTitle()
                                + "\n\n"
                                + "- Source: " + request.source() + "\n"
                                + "- Language: " + request.language() + "\n\n"
                                + pendingMarker + "\n";

                GitHubPutFileRequest readmeRequest =
                        new GitHubPutFileRequest(
                                "Prepare sync: " + request.problemTitle(),
                                encode(readmeContent),
                                branch,
                                existingProblemReadme == null
                                        ? null : existingProblemReadme.sha()
                        );

                // Persist the recovery marker before creating the solution.
                githubClient.putFile(
                        token, owner, repo,
                        problemReadmePath, readmeRequest
                );
            }

            GitHubPutFileRequest putRequest =
                    new GitHubPutFileRequest(
                            message,
                            encode(formattedCode),
                            branch,
                            null
                    );

            GitHubPutFileResponse result =
                    githubClient.putFile(
                            token, owner, repo, solutionPath, putRequest
                    );

            String sha = extractCommitSha(result);

            // The counted marker makes this operation idempotent on retries.
            if (shouldCountProblem) {
                updateRootReadme(
                        token, owner, repo, branch,
                        verifiedRootReadme, request
                );

                GitHubContentsResponse pendingReadme =
                        githubClient.getFile(
                                token, owner, repo,
                                problemReadmePath, branch
                        );

                removePendingMarker(
                        token, owner, repo, request,
                        branch, pendingReadme
                );
            }

            return new SyncResponse(
                    "CREATED", message, solutionPath, sha, true
            );
        }

        // If a previous sync created the solution but failed while
        // updating global stats, finish that update before returning.
        if (pendingCount) {
            updateRootReadme(
                    token, owner, repo, branch,
                    verifiedRootReadme, request
            );

            GitHubContentsResponse pendingReadme =
                    githubClient.getFile(
                            token, owner, repo,
                            problemReadmePath, branch
                    );

            removePendingMarker(
                    token, owner, repo, request,
                    branch, pendingReadme
            );
        }

        String existingFile = decode(existingSolution.content());
        String existingCode = formatter.extractCode(existingFile);

        // Repair a missing problem README without duplicating the solution.
        if (!problemReadmeExists) {
            createOrUpdateProblemReadme(
                    token, owner, repo, request,
                    branch, existingProblemReadme
            );
        }

        if (existingCode.equals(normalizeCode(request.code()))) {
            return SyncResponse.skipped(
                    "Exact same solution already exists; "
                            + "no duplicate solution commit created.",
                    solutionPath
            );
        }

        boolean shouldUpdate =
                shouldUpdateSolution(request, existingFile);

        if (!shouldUpdate) {
            return SyncResponse.skipped(
                    "Different solution found, but the existing "
                            + "solution is not outperformed.",
                    solutionPath
            );
        }

        String message = buildCommitMessage(request, true);

        GitHubPutFileRequest putRequest =
                new GitHubPutFileRequest(
                        message,
                        encode(formattedCode),
                        branch,
                        existingSolution.sha()
                );

        GitHubPutFileResponse result =
                githubClient.putFile(
                        token, owner, repo, solutionPath, putRequest
                );

        String sha = extractCommitSha(result);

        if (problemReadmeExists) {
            createOrUpdateProblemReadme(
                    token, owner, repo, request,
                    branch, existingProblemReadme
            );
        }

        return new SyncResponse(
                "UPDATED", message, solutionPath, sha, true
        );
    }

    private String getLegacySolutionPath(
            SubmissionRequest request
    ) {
        if (request.source() != SubmissionRequest.Source.LEETCODE) {
            return null;
        }

        String numericId =
                request.problemId().replaceAll("\\D", "");

        if (numericId.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PROBLEM_ID",
                    "The LeetCode problem ID must contain a number."
            );
        }

        try {
            int problemNumber = Integer.parseInt(numericId);

            return legacyPathService.generatePath(
                    problemNumber,
                    request.problemTitle(),
                    request.language()
            );
        } catch (NumberFormatException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PROBLEM_ID",
                    "The LeetCode problem ID is outside the supported range."
            );
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "UNSUPPORTED_LANGUAGE",
                    exception.getMessage()
            );
        }
    }

    private void createOrUpdateProblemReadme(
            String token,
            String owner,
            String repo,
            SubmissionRequest request,
            String branch,
            GitHubContentsResponse existingReadme
    ) {
        String path = codeFileService.problemReadmePath(request);

        String content;

        if (existingReadme != null
                && existingReadme.content() != null) {
            content = decode(existingReadme.content());
        } else {
            content = "# "
                    + request.problemId()
                    + ". "
                    + request.problemTitle()
                    + "\n\n"
                    + "- Source: " + request.source() + "\n"
                    + "- Language: " + request.language() + "\n";
        }

        GitHubPutFileRequest putRequest =
                new GitHubPutFileRequest(
                        "Update README: "
                                + request.problemId()
                                + ". "
                                + request.problemTitle(),
                        encode(content),
                        branch,
                        existingReadme == null
                                ? null : existingReadme.sha()
                );

        githubClient.putFile(
                token, owner, repo, path, putRequest
        );
    }

    private void removePendingMarker(
            String token,
            String owner,
            String repo,
            SubmissionRequest request,
            String branch,
            GitHubContentsResponse existingReadme
    ) {
        if (existingReadme == null
                || existingReadme.content() == null
                || existingReadme.sha() == null) {
            return;
        }

        String content = decode(existingReadme.content());
        String marker = pendingProblemMarker(request);

        if (!content.contains(marker)) {
            return;
        }

        content = content.replace(marker, "").stripTrailing() + "\n";

        GitHubPutFileRequest putRequest =
                new GitHubPutFileRequest(
                        "Complete sync recovery: "
                                + request.problemId()
                                + ". "
                                + request.problemTitle(),
                        encode(content),
                        branch,
                        existingReadme.sha()
                );

        githubClient.putFile(
                token,
                owner,
                repo,
                codeFileService.problemReadmePath(request),
                putRequest
        );
    }

    private GitHubContentsResponse verifyRootReadme(
            String token,
            String owner,
            String repo,
            String branch
    ) {
        GitHubContentsResponse rootReadme =
                githubClient.getFile(
                        token, owner, repo, "README.md", branch
                );

        if (rootReadme == null
                || rootReadme.content() == null
                || rootReadme.path() == null
                || !rootReadme.path().equalsIgnoreCase("README.md")
                || rootReadme.sha() == null
                || rootReadme.sha().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "ROOT_README_READ_ERROR",
                    "Could not verify the existing root README. "
                            + "Refusing to create files for a new problem."
            );
        }

        try {
            decode(rootReadme.content());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "ROOT_README_READ_ERROR",
                    "The existing root README could not be decoded. "
                            + "Refusing to overwrite it."
            );
        }

        return rootReadme;
    }

    private String countedProblemMarker(SubmissionRequest request) {
        String problemId = request.problemId()
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        return "<!-- LEETSYNC:COUNTED:"
                + request.source()
                + ":"
                + problemId
                + " -->";
    }

    private String pendingProblemMarker(SubmissionRequest request) {
        String problemId = request.problemId()
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        return "<!-- LEETSYNC:PENDING:"
                + request.source()
                + ":"
                + problemId
                + " -->";
    }

    private void updateRootReadme(
            String token,
            String owner,
            String repo,
            String branch,
            GitHubContentsResponse existingRootReadme,
            SubmissionRequest request
    ) {
        String content = decode(existingRootReadme.content());
        String marker = countedProblemMarker(request);

        // If GitHub committed an earlier attempt but its response was lost,
        // the marker prevents the problem count from being incremented again.
        if (content.contains(marker)) {
            return;
        }

        Pattern pattern = Pattern.compile(
                "Global stats:\\s*(\\d+)\\s+"
                        + "Problems?\\s+Solved",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(content);
        boolean statsExist = matcher.find();

        int solvedCount = statsExist
                ? Integer.parseInt(matcher.group(1))
                : 0;

        solvedCount++;

        String problemWord = solvedCount == 1
                ? "Problem" : "Problems";

        String statsLine = "Global stats: "
                + solvedCount + " " + problemWord + " Solved";

        matcher = pattern.matcher(content);

        if (matcher.find()) {
            content = matcher.replaceFirst(
                    Matcher.quoteReplacement(statsLine)
            );
        } else {
            content = content.stripTrailing()
                    + "\n\n" + statsLine + "\n";
        }

        content = content.stripTrailing()
                + "\n\n" + marker + "\n";

        GitHubPutFileRequest putRequest =
                new GitHubPutFileRequest(
                        "Update global stats",
                        encode(content),
                        branch,
                        existingRootReadme.sha()
                );

        githubClient.putFile(
                token, owner, repo, "README.md", putRequest
        );
    }

    private boolean shouldUpdateSolution(
            SubmissionRequest request,
            String existingFile
    ) {
        Performance existingPerformance =
                extractPerformance(existingFile);

        Double newRuntime = request.runtimePercentile();
        Double newMemory = request.memoryPercentile();

        if (newRuntime == null && newMemory == null) {
            return true;
        }

        if (existingPerformance.runtime == null
                && existingPerformance.memory == null) {
            return true;
        }

        if (newRuntime != null && newMemory != null
                && existingPerformance.runtime != null
                && existingPerformance.memory != null) {

            boolean runtimeAtLeastAsGood =
                    newRuntime >= existingPerformance.runtime;

            boolean memoryAtLeastAsGood =
                    newMemory >= existingPerformance.memory;

            boolean strictlyBetter =
                    newRuntime > existingPerformance.runtime
                            || newMemory > existingPerformance.memory;

            return runtimeAtLeastAsGood
                    && memoryAtLeastAsGood
                    && strictlyBetter;
        }

        if (newRuntime != null
                && existingPerformance.runtime != null) {
            return newRuntime > existingPerformance.runtime;
        }

        if (newMemory != null
                && existingPerformance.memory != null) {
            return newMemory > existingPerformance.memory;
        }

        return true;
    }

    private Performance extractPerformance(String file) {
        Double runtime = extractPercentile(file, "// Runtime:");
        Double memory = extractPercentile(file, "// Memory:");

        return new Performance(runtime, memory);
    }

    private Double extractPercentile(
            String file,
            String prefix
    ) {
        for (String line : file.split("\n")) {
            if (!line.startsWith(prefix)) {
                continue;
            }

            int beatsIndex = line.indexOf("Beats ");

            if (beatsIndex == -1) {
                return null;
            }

            String value = line.substring(
                    beatsIndex + "Beats ".length()
            ).replace("%", "").trim();

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private String normalizeCode(String code) {
        return code.replace("\r\n", "\n").trim();
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String base64) {
        String normalized = base64.replaceAll("\\s", "");

        return new String(
                Base64.getDecoder().decode(normalized),
                StandardCharsets.UTF_8
        );
    }

    private String buildCommitMessage(
            SubmissionRequest request,
            boolean update
    ) {
        String verb = update ? "Updated" : "Solved";

        return "Auto-commit: "
                + verb + " "
                + request.problemId() + ". "
                + request.problemTitle();
    }

    private String extractCommitSha(
            GitHubPutFileResponse result
    ) {
        if (result == null || result.commit() == null) {
            return null;
        }

        return result.commit().sha();
    }

    private record Performance(
            Double runtime,
            Double memory
    ) {
    }
}