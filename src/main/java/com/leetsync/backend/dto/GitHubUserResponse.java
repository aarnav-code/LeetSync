package com.leetsync.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubUserResponse(
        String login,
        String name,
        @JsonProperty("avatar_url") String avatarUrl
) {}
