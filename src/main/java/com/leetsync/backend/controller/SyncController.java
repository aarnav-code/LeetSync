package com.leetsync.backend.controller;

import com.leetsync.backend.dto.SubmissionRequest;
import com.leetsync.backend.dto.SyncResponse;
import com.leetsync.backend.exception.ApiException;
import com.leetsync.backend.service.GitHubSyncService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final GitHubSyncService syncService;

    public SyncController(GitHubSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/submission")
    public SyncResponse sync(@RequestHeader("Authorization") String authorization,
                             @RequestHeader("X-GitHub-Owner") String owner,
                             @Valid @RequestBody SubmissionRequest request) {
        return syncService.sync(extractBearer(authorization), owner, request);
    }

    @PostMapping("/leetcode")
    public SyncResponse leetcode(@RequestHeader("Authorization") String authorization,
                                 @RequestHeader("X-GitHub-Owner") String owner,
                                 @Valid @RequestBody SubmissionRequest request) {
        assertSource(request, SubmissionRequest.Source.LEETCODE);
        return syncService.sync(extractBearer(authorization), owner, request);
    }

    @PostMapping("/codeforces")
    public SyncResponse codeforces(@RequestHeader("Authorization") String authorization,
                                   @RequestHeader("X-GitHub-Owner") String owner,
                                   @Valid @RequestBody SubmissionRequest request) {
        assertSource(request, SubmissionRequest.Source.CODEFORCES);
        return syncService.sync(extractBearer(authorization), owner, request);
    }

    @PostMapping("/gfg")
    public SyncResponse gfg(@RequestHeader("Authorization") String authorization,
                            @RequestHeader("X-GitHub-Owner") String owner,
                            @Valid @RequestBody SubmissionRequest request) {
        assertSource(request, SubmissionRequest.Source.GEEKS_FOR_GEEKS);
        return syncService.sync(extractBearer(authorization), owner, request);
    }

    private void assertSource(SubmissionRequest request, SubmissionRequest.Source expected) {
        if (request.source() != expected) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SOURCE_MISMATCH",
                    "Endpoint and submission source do not match.");
        }
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization;
        }
        return authorization.substring(7).trim();
    }
}
