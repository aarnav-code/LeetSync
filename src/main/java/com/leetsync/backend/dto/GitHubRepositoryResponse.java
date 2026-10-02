package com.leetsync.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryResponse(
        String name,
        @JsonProperty("default_branch") String defaultBranch,
        boolean fork,
        boolean archived,
        String visibility,
        Owner owner
) {
    public record Owner(String login) {}
}
