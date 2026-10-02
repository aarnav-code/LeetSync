package com.leetsync.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubFileResponse(
        String content,
        String sha,
        @JsonProperty("download_url") String downloadUrl
) {}
