package com.leetsync.backend.dto;

public record AuthValidationResponse(
        boolean valid,
        String login,
        String name,
        String avatarUrl
) {}
