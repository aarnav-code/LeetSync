package com.leetsync.backend.service;

import com.leetsync.backend.dto.GitHubContentsResponse;
import com.leetsync.backend.dto.GitHubPutFileRequest;
import com.leetsync.backend.dto.GitHubPutFileResponse;
import com.leetsync.backend.dto.GitHubRepositoryResponse;
import com.leetsync.backend.dto.GitHubSolutionRequest;
import com.leetsync.backend.dto.GitHubUserResponse;
import com.leetsync.backend.github.GitHubClient;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class GitHubService {

    private final GitHubClient gitHubClient;
    private final GitHubPathService gitHubPathService;
    private final String defaultBranch;

    public GitHubService(
            GitHubClient gitHubClient,
            GitHubPathService gitHubPathService,
            @Value("${app.github.default-branch}") String defaultBranch
    ) {
        this.gitHubClient = gitHubClient;
        this.gitHubPathService = gitHubPathService;
        this.defaultBranch = defaultBranch;
    }

    public GitHubUserResponse getAuthenticatedUser(String token) {
        return gitHubClient.getAuthenticatedUser(token);
    }

    public GitHubRepositoryResponse getRepository(
            String token,
            String owner,
            String repo
    ) {
        return gitHubClient.getRepository(token, owner, repo);
    }

    public GitHubContentsResponse getFile(
            String token,
            String owner,
            String repo,
            String path,
            String branch
    ) {
        return gitHubClient.getFile(
                token,
                owner,
                repo,
                path,
                branch
        );
    }

    public GitHubPutFileResponse putFile(
            String token,
            String owner,
            String repo,
            String path,
            GitHubPutFileRequest request
    ) {
        return gitHubClient.putFile(
                token,
                owner,
                repo,
                path,
                request
        );
    }

    public GitHubPutFileResponse syncSolution(
            String token,
            String owner,
            String repo,
            GitHubSolutionRequest request
    ) {
        String branch = defaultBranch;

        String solutionPath = gitHubPathService.generatePath(
                request.getProblemNumber(),
                request.getProblemTitle(),
                request.getLanguage()
        );

        String problemReadmePath =
                gitHubPathService.generateProblemReadmePath(
                        request.getProblemNumber(),
                        request.getProblemTitle()
                );

        String encodedContent = Base64.getEncoder()
                .encodeToString(
                        request.getCode().getBytes(StandardCharsets.UTF_8)
                );

        GitHubContentsResponse existingSolution = gitHubClient.getFile(
                token,
                owner,
                repo,
                solutionPath,
                branch
        );

        GitHubContentsResponse existingProblemReadme =
                gitHubClient.getFile(
                        token,
                        owner,
                        repo,
                        problemReadmePath,
                        branch
                );

        boolean newProblem =
                existingProblemReadme == null
                        || existingProblemReadme.content() == null;

        String sha = existingSolution != null
                ? existingSolution.sha()
                : null;

        String commitMessage = existingSolution == null
                ? "Add solution: "
                : "Update solution: ";

        commitMessage += request.getProblemNumber()
                + ". "
                + request.getProblemTitle();

        GitHubPutFileRequest solutionRequest =
                new GitHubPutFileRequest(
                        commitMessage,
                        encodedContent,
                        branch,
                        sha
                );

        GitHubPutFileResponse result =
                gitHubClient.putFile(
                        token,
                        owner,
                        repo,
                        solutionPath,
                        solutionRequest
                );

        // Create the problem README when this is a new problem.
        if (newProblem) {
            String readmeContent =
                    "# "
                            + request.getProblemNumber()
                            + ". "
                            + request.getProblemTitle()
                            + "\n\n"
                            + "- Language: "
                            + request.getLanguage()
                            + "\n";

            String encodedReadme = Base64.getEncoder()
                    .encodeToString(
                            readmeContent.getBytes(StandardCharsets.UTF_8)
                    );

            GitHubPutFileRequest readmeRequest =
                    new GitHubPutFileRequest(
                            "Add README: "
                                    + request.getProblemNumber()
                                    + ". "
                                    + request.getProblemTitle(),
                            encodedReadme,
                            branch,
                            existingProblemReadme == null
                                    ? null
                                    : existingProblemReadme.sha()
                    );

            gitHubClient.putFile(
                    token,
                    owner,
                    repo,
                    problemReadmePath,
                    readmeRequest
            );
        }

        updateRootReadme(
                token,
                owner,
                repo,
                branch,
                request
        );

        return result;
    }

    private void updateRootReadme(
            String token,
            String owner,
            String repo,
            String branch,
            GitHubSolutionRequest request
    ) {
        String path = "README.md";

        GitHubContentsResponse existingRootReadme =
                gitHubClient.getFile(
                        token,
                        owner,
                        repo,
                        path,
                        branch
                );

        String currentProblemDirectory =
                String.format(
                        "%04d-%s",
                        request.getProblemNumber(),
                        request.getProblemTitle()
                                .toLowerCase()
                                .trim()
                                .replaceAll("[^a-z0-9]+", "-")
                                .replaceAll("^-|-$", "")
                );

        String solutionFile =
                "solution"
                        + switch (request.getLanguage().trim().toLowerCase()) {
                    case "java" -> ".java";
                    case "c++", "cpp" -> ".cpp";
                    case "python", "python3" -> ".py";
                    case "javascript", "js" -> ".js";
                    case "typescript", "ts" -> ".ts";
                    case "c" -> ".c";
                    case "c#" -> ".cs";
                    case "go" -> ".go";
                    case "rust" -> ".rs";
                    case "kotlin" -> ".kt";
                    case "swift" -> ".swift";
                    default -> ".txt";
                };

        java.util.Set<String> problems =
                new java.util.TreeSet<>();

        if (existingRootReadme != null
                && existingRootReadme.content() != null) {

            String existingContent = new String(
                    Base64.getDecoder().decode(
                            existingRootReadme.content()
                                    .replaceAll("\\s", "")
                    ),
                    StandardCharsets.UTF_8
            );

            java.util.regex.Pattern pattern =
                    java.util.regex.Pattern.compile(
                            "[├└]──\\s+(\\d{4}-[a-z0-9-]+)"
                    );

            java.util.regex.Matcher matcher =
                    pattern.matcher(existingContent);

            while (matcher.find()) {
                problems.add(matcher.group(1));
            }
        }

        problems.add(currentProblemDirectory);

        StringBuilder tree = new StringBuilder();

        tree.append("# Solved LeetCode Problems\n\n");
        tree.append("```text\n");

        int index = 0;

        for (String problem : problems) {
            boolean last = index == problems.size() - 1;

            tree.append(last ? "└── " : "├── ")
                    .append(problem)
                    .append("\n");

            tree.append(last ? "    ├── " : "│   ├── ")
                    .append(
                            problem.equals(currentProblemDirectory)
                                    ? solutionFile
                                    : "solution.java"
                    )
                    .append("\n");

            tree.append(last ? "    └── " : "│   └── ")
                    .append("README.md")
                    .append("\n");

            if (!last) {
                tree.append("\n");
            }

            index++;
        }

        tree.append("```\n");

        String encodedContent =
                Base64.getEncoder()
                        .encodeToString(
                                tree.toString()
                                        .getBytes(StandardCharsets.UTF_8)
                        );

        GitHubPutFileRequest rootReadmeRequest =
                new GitHubPutFileRequest(
                        "Update solved problems tree",
                        encodedContent,
                        branch,
                        existingRootReadme == null
                                ? null
                                : existingRootReadme.sha()
                );

        gitHubClient.putFile(
                token,
                owner,
                repo,
                path,
                rootReadmeRequest
        );
    }
}