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

        String path = gitHubPathService.generatePath(
                request.getProblemNumber(),
                request.getProblemTitle(),
                request.getLanguage()
        );

        String encodedContent = Base64.getEncoder()
                .encodeToString(
                        request.getCode().getBytes(StandardCharsets.UTF_8)
                );

        GitHubContentsResponse existingFile = gitHubClient.getFile(
                token,
                owner,
                repo,
                path,
                branch
        );

        String sha = existingFile != null
                ? existingFile.sha()
                : null;

        String commitMessage = existingFile == null
                ? "Add solution: "
                : "Update solution: ";

        commitMessage += request.getProblemNumber()
                + ". "
                + request.getProblemTitle();

        GitHubPutFileRequest putRequest = new GitHubPutFileRequest(
                commitMessage,
                encodedContent,
                branch,
                sha
        );

        return gitHubClient.putFile(
                token,
                owner,
                repo,
                path,
                putRequest
        );
    }
}