package com.leetsync.backend.dto;

public record GitHubPutFileRequest(
        String message,
        String content,
        String branch,
        String sha
) {}
