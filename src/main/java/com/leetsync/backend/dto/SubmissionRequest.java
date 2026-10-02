package com.leetsync.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmissionRequest(
        @NotNull Source source,
        @NotBlank String problemId,
        @NotBlank String problemTitle,
        @NotBlank String language,
        @NotBlank String code,
        String repository,
        String branch,
        String runtime,
        Double runtimePercentile,
        String memory,
        Double memoryPercentile,
        Boolean accepted
) {
    public enum Source {
        LEETCODE,
        GEEKS_FOR_GEEKS,
        CODEFORCES
    }
}
