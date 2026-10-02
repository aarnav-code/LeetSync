package com.leetsync.backend.github;

import com.leetsync.backend.dto.*;
import com.leetsync.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class GitHubClient {

    private final WebClient client;

    public GitHubClient(WebClient githubWebClient) {
        this.client = githubWebClient;
    }

    public GitHubUserResponse getAuthenticatedUser(String token) {
        return client.get()
                .uri("/user")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(body ->
                                Mono.error(toGitHubException(
                                        response.statusCode(),
                                        "GITHUB_REQUEST_FAILED",
                                        "GitHub rejected the authentication request."
                                ))))
                .bodyToMono(GitHubUserResponse.class)
                .block();
    }

    public GitHubRepositoryResponse getRepository(
            String token,
            String owner,
            String repo
    ) {
        return client.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(body ->
                                Mono.error(toGitHubException(
                                        response.statusCode(),
                                        "GITHUB_REPOSITORY_ERROR",
                                        response.statusCode().value() == 404
                                                ? "Repository was not found or is not accessible."
                                                : "GitHub repository request failed."
                                ))))
                .bodyToMono(GitHubRepositoryResponse.class)
                .block();
    }

    public GitHubContentsResponse getFile(
            String token,
            String owner,
            String repo,
            String path,
            String branch
    ) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents/")
                        .path(path)
                        .queryParam("ref", branch)
                        .build(owner, repo))
                .headers(h -> h.setBearerAuth(token))
                .exchangeToMono(response -> {

                    // 404 means the solution doesn't exist yet.
                    if (response.statusCode().value() == 404) {
                        return Mono.empty();
                    }

                    if (response.statusCode().value() == 401) {
                        return Mono.error(new ApiException(
                                HttpStatus.UNAUTHORIZED,
                                "GITHUB_TOKEN_INVALID",
                                "GitHub token is invalid or expired."
                        ));
                    }

                    if (response.statusCode().isError()) {
                        return Mono.error(new ApiException(
                                HttpStatus.BAD_GATEWAY,
                                "GITHUB_FILE_READ_ERROR",
                                "GitHub file lookup failed."
                        ));
                    }

                    return response.bodyToMono(GitHubContentsResponse.class);
                })
                .block();
    }

    public GitHubPutFileResponse putFile(
            String token,
            String owner,
            String repo,
            String path,
            GitHubPutFileRequest request
    ) {
        return client.put()
                .uri(
                        "/repos/{owner}/{repo}/contents/{path}",
                        owner,
                        repo,
                        path
                )
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(body ->
                                Mono.error(toGitHubException(
                                        response.statusCode(),
                                        "GITHUB_FILE_WRITE_ERROR",
                                        "GitHub rejected the file write."
                                ))))
                .bodyToMono(GitHubPutFileResponse.class)
                .block();
    }

    /**
     * Converts GitHub HTTP errors into our application's ApiException.
     * Authentication errors are kept distinct from repository/file errors
     * so the extension can specifically request re-authentication on 401.
     */
    private ApiException toGitHubException(
            HttpStatusCode statusCode,
            String defaultCode,
            String defaultMessage
    ) {
        if (statusCode.value() == 401) {
            return new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "GITHUB_TOKEN_INVALID",
                    "GitHub token is invalid or expired."
            );
        }

        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                defaultCode,
                defaultMessage
        );
    }
}