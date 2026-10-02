package com.leetsync.backend.dto;

public record GitHubContentsResponse(
        String content,
        String sha,
        String path
) {}
