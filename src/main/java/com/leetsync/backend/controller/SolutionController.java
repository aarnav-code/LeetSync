package com.leetsync.backend.controller;

import com.leetsync.backend.dto.SolutionRequest;
import com.leetsync.backend.entity.Solution;
import com.leetsync.backend.service.SolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.leetsync.backend.dto.GitHubContentsResponse;
import com.leetsync.backend.dto.GitHubPutFileRequest;
import com.leetsync.backend.dto.GitHubPutFileResponse;
import com.leetsync.backend.dto.GitHubRepositoryResponse;
import com.leetsync.backend.dto.GitHubUserResponse;
import com.leetsync.backend.service.GitHubService;
import com.leetsync.backend.dto.GitHubSolutionRequest;
import com.leetsync.backend.dto.SubmissionRequest;
import com.leetsync.backend.dto.SyncResponse;
import com.leetsync.backend.service.GitHubSyncService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/solutions")
public class SolutionController {

    private final SolutionService solutionService;
    private final GitHubService gitHubService;
    private final GitHubSyncService gitHubSyncService;

    public SolutionController(
            SolutionService solutionService,
            GitHubService gitHubService,
            GitHubSyncService gitHubSyncService
    ) {
        this.solutionService = solutionService;
        this.gitHubService = gitHubService;
        this.gitHubSyncService = gitHubSyncService;
    }

    @PostMapping
    public Solution createSolution(@Valid @RequestBody SolutionRequest request) {
        return solutionService.saveSolution(request);
    }

    @GetMapping
    public List<Solution> getAllSolutions() {
        return solutionService.getAllSolutions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Solution> getSolutionById(@PathVariable Long id) {
        return solutionService.getSolutionById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSolution(@PathVariable Long id) {
        boolean deleted = solutionService.deleteSolution(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Solution> updateSolution(
            @PathVariable Long id,
            @Valid @RequestBody SolutionRequest request
    ) {
        return solutionService.updateSolution(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/github/user")
    public GitHubUserResponse getGitHubUser(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.replace("Bearer ", "");

        return gitHubService.getAuthenticatedUser(token);
    }

    @GetMapping("/github/repository/{owner}/{repo}")
    public GitHubRepositoryResponse getGitHubRepository(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String owner,
            @PathVariable String repo
    ) {
        String token = authorization.replace("Bearer ", "");

        return gitHubService.getRepository(token, owner, repo);
    }

    @GetMapping("/github/repository/{owner}/{repo}/file")
    public ResponseEntity<GitHubContentsResponse> getGitHubFile(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String path,
            @RequestParam(defaultValue = "main") String branch
    ) {
        String token = authorization.replace("Bearer ", "");

        GitHubContentsResponse file = gitHubService.getFile(
                token,
                owner,
                repo,
                path,
                branch
        );

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(file);
    }

    @PutMapping("/github/repository/{owner}/{repo}/file")
    public GitHubPutFileResponse putGitHubFile(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String path,
            @RequestBody GitHubPutFileRequest request
    ) {
        String token = authorization.replace("Bearer ", "");

        return gitHubService.putFile(
                token,
                owner,
                repo,
                path,
                request
        );
    }

    @PostMapping("/github/sync/{owner}/{repo}")
    public SyncResponse syncSolution(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String owner,
            @PathVariable String repo,
            @Valid @RequestBody GitHubSolutionRequest request
    ) {
        String token = authorization.replace("Bearer ", "");

        SubmissionRequest submission = new SubmissionRequest(
                SubmissionRequest.Source.LEETCODE,
                String.valueOf(request.getProblemNumber()),
                request.getProblemTitle(),
                request.getLanguage(),
                request.getCode(),
                repo,
                null,
                null,
                null,
                null,
                null,
                true
        );

        return gitHubSyncService.sync(token, owner, submission);
    }
}