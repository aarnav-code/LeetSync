package com.leetsync.backend.service;

import com.leetsync.backend.dto.AuthValidationResponse;
import com.leetsync.backend.dto.GitHubUserResponse;
import com.leetsync.backend.exception.ApiException;
import com.leetsync.backend.github.GitHubClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final GitHubClient githubClient;

    public AuthService(GitHubClient githubClient) {
        this.githubClient = githubClient;
    }

    public AuthValidationResponse validate(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MISSING_TOKEN", "GitHub access token is required.");
        }
        GitHubUserResponse user = githubClient.getAuthenticatedUser(token);
        return new AuthValidationResponse(true, user.login(), user.name(), user.avatarUrl());
    }
}
