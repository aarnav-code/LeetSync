package com.leetsync.backend.dto;

public record RepositoryInfo(
        String owner,
        String name,
        String defaultBranch,
        boolean privateRepository
) {}
