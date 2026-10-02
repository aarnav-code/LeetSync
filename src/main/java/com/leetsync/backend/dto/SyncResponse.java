package com.leetsync.backend.dto;

public record SyncResponse(
        String status,
        String message,
        String path,
        String commitSha,
        boolean changed
) {
    public static SyncResponse skipped(String message, String path) {
        return new SyncResponse("SKIPPED", message, path, null, false);
    }
}
