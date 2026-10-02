package com.leetsync.backend.dto;

public record GitHubPutFileResponse(Commit commit) {
    public record Commit(String sha) {}
}
