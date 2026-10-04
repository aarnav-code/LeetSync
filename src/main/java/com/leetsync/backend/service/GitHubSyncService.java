package com.leetsync.backend.service;

import com.leetsync.backend.dto.*;
import com.leetsync.backend.exception.ApiException;
import com.leetsync.backend.github.GitHubClient;
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

    public GitHubSyncService(
            GitHubClient githubClient,
            CodeFileService codeFileService,
            SubmissionFormatter formatter
    ) {
        this.githubClient = githubClient;
        this.codeFileService = codeFileService;
        this.formatter = formatter;
    }

    public SyncResponse sync(
            String token,
            String owner,
            SubmissionRequest request
    ) {

        // Only accepted submissions should reach GitHub.
        if (request.accepted() != null && !request.accepted()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NOT_ACCEPTED",
                    "Only accepted submissions can be synchronized."
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

        // Verify the repository and determine the target branch.
        GitHubRepositoryResponse repository =
                githubClient.getRepository(token, owner, repo);

        String branch = request.branch() == null
                || request.branch().isBlank()
                ? repository.defaultBranch()
                : request.branch();

        // Generate paths for this problem.
        String solutionPath = codeFileService.path(request);
        String problemReadmePath =
                codeFileService.problemReadmePath(request);

        // Check both files independently.
        GitHubContentsResponse existingSolution =
                githubClient.getFile(
                        token, owner, repo, solutionPath, branch
                );

        GitHubContentsResponse existingProblemReadme =
                githubClient.getFile(
                        token, owner, repo, problemReadmePath, branch
                );

        boolean solutionExists = existingSolution != null
                && existingSolution.content() != null;

        boolean problemReadmeExists = existingProblemReadme != null
                && existingProblemReadme.content() != null;

        // A problem is new only when neither file exists.
        boolean isNewProblem =
                !solutionExists && !problemReadmeExists;

        /*
         * Verify the root README before creating files for a new
         * problem. Reuse this exact response for the later update.
         */
        GitHubContentsResponse verifiedRootReadme = null;

        if (isNewProblem) {
            verifiedRootReadme = verifyRootReadme(
                    token, owner, repo, branch
            );
        }

        String formattedCode = formatter.format(request);

        // Handle a missing solution file.
        if (!solutionExists) {

            String message = buildCommitMessage(request, false);

            GitHubPutFileRequest putRequest =
                    new GitHubPutFileRequest(
                            message,
                            encode(formattedCode),
                            branch,
                            null
                    );

            GitHubPutFileResponse result =
                    githubClient.putFile(
                            token,
                            owner,
                            repo,
                            solutionPath,
                            putRequest
                    );

            String sha = extractCommitSha(result);

            // Create or repair the problem README independently.
            if (!problemReadmeExists) {
                createOrUpdateProblemReadme(
                        token,
                        owner,
                        repo,
                        request,
                        branch,
                        existingProblemReadme
                );
            }

            // Increment the count only for a genuinely new problem.
            if (isNewProblem) {
                updateRootReadme(
                        token,
                        owner,
                        repo,
                        branch,
                        verifiedRootReadme
                );
            }

            return new SyncResponse(
                    "CREATED",
                    message,
                    solutionPath,
                    sha,
                    true
            );
        }

        // Decode the existing solution file.
        String existingFile = decode(existingSolution.content());

        String existingCode = formatter.extractCode(existingFile);

        /*
         * Repair a missing problem README before any early return.
         * This also handles identical solutions without creating
         * another solution commit.
         */
        if (!problemReadmeExists) {
            createOrUpdateProblemReadme(
                    token,
                    owner,
                    repo,
                    request,
                    branch,
                    existingProblemReadme
            );
        }

        // Identical source code must not create a duplicate commit.
        if (existingCode.equals(normalizeCode(request.code()))) {
            return SyncResponse.skipped(
                    "Exact same solution already exists; "
                            + "no duplicate solution commit created.",
                    solutionPath
            );
        }

        /*
         * If performance information is available, only replace the
         * existing solution when the new solution is demonstrably
         * better based on the available metrics.
         *
         * If no performance information is available, allow the
         * different solution to be stored.
         */
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
                        token,
                        owner,
                        repo,
                        solutionPath,
                        putRequest
                );

        String sha = extractCommitSha(result);

        // Existing problems never increase the global solved count.
        // Update their README only when it already existed.
        if (problemReadmeExists) {
            createOrUpdateProblemReadme(
                    token,
                    owner,
                    repo,
                    request,
                    branch,
                    existingProblemReadme
            );
        }

        return new SyncResponse(
                "UPDATED",
                message,
                solutionPath,
                sha,
                true
        );
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

        String content = "# "
                + request.problemId()
                + ". "
                + request.problemTitle()
                + "\n\n"
                + "- Source: " + request.source() + "\n"
                + "- Language: " + request.language() + "\n";

        GitHubPutFileRequest putRequest =
                new GitHubPutFileRequest(
                        "Update README: "
                                + request.problemId()
                                + ". "
                                + request.problemTitle(),
                        encode(content),
                        branch,
                        existingReadme == null
                                ? null
                                : existingReadme.sha()
                );

        githubClient.putFile(
                token,
                owner,
                repo,
                path,
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

        // Ensure the returned content is valid Base64 before proceeding.
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

    private void updateRootReadme(
            String token,
            String owner,
            String repo,
            String branch,
            GitHubContentsResponse existingRootReadme
    ) {
        // Use the already-verified response. Do not fetch the file again.
        String content = decode(existingRootReadme.content());

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
                ? "Problem"
                : "Problems";

        String statsLine = "Global stats: "
                + solvedCount
                + " "
                + problemWord
                + " Solved";

        matcher = pattern.matcher(content);

        if (matcher.find()) {
            content = matcher.replaceFirst(
                    Matcher.quoteReplacement(statsLine)
            );
        } else {
            content = content.stripTrailing()
                    + "\n\n"
                    + statsLine
                    + "\n";
        }

        GitHubPutFileRequest putRequest =
                new GitHubPutFileRequest(
                        "Update global stats",
                        encode(content),
                        branch,
                        existingRootReadme.sha()
                );

        githubClient.putFile(
                token,
                owner,
                repo,
                "README.md",
                putRequest
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

    private Double extractPercentile(String file, String prefix) {
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
        return Base64.getEncoder()
                .encodeToString(
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
                + verb
                + " "
                + request.problemId()
                + ". "
                + request.problemTitle();
    }

    private String extractCommitSha(GitHubPutFileResponse result) {
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