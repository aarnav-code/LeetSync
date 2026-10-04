
package com.leetsync.backend.service;

import com.leetsync.backend.dto.*;
import com.leetsync.backend.github.GitHubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.leetsync.backend.exception.ApiException;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubSyncServiceTest {

    @Mock
    private GitHubClient githubClient;

    private CodeFileService codeFileService;
    private SubmissionFormatter formatter;
    private GitHubSyncService service;

    private final String token = "test-token";
    private final String owner = "test-owner";
    private final String repo = "LeetSync";
    private final String branch = "main";

    @BeforeEach
    void setUp() {
        codeFileService = new CodeFileService();
        formatter = new SubmissionFormatter();
        service = new GitHubSyncService(
                githubClient,
                codeFileService,
                formatter
        );

        when(githubClient.getRepository(token, owner, repo))
                .thenReturn(new GitHubRepositoryResponse(
                        repo, branch, false, false, "private", null
                ));
    }

    @Test
    void createsNewProblemAndUpdatesGlobalStats() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                contains("solution.java"), eq(branch)
        )).thenReturn(null);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> path != null
                        && path.endsWith("/README.md")
                        && !path.equals("README.md")),
                eq(branch)
        )).thenReturn(null);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode("# LeetSync\n\nMy project documentation.\n"),
                "root-readme-sha",
                "README.md"
        ));

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("test-sha")
        ));

        SyncResponse response = service.sync(token, owner, request);

        assertEquals("CREATED", response.status());

        verify(githubClient, times(3)).putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        );

        verify(githubClient).putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"),
                argThat(body -> {
                    String updated = decode(body.content());
                    return updated.contains("My project documentation.")
                            && updated.contains("Global stats: 1 Problem Solved");
                })
        );
    }

    @Test
    void createsMissingSolutionWithoutIncrementingGlobalStats() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                contains("solution.java"), eq(branch)
        )).thenReturn(null);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> path != null
                        && path.endsWith("/README.md")
                        && !path.equals("README.md")),
                eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode("# 1. Two Sum"), "readme-sha",
                "0001-two-sum/README.md"
        ));

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("test-sha")
        ));

        SyncResponse response = service.sync(token, owner, request);

        assertEquals("CREATED", response.status());

        verify(githubClient, times(1)).putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        );

        verify(githubClient, never()).putFile(
                eq(token), eq(owner), eq(repo), eq("README.md"), any()
        );
    }

    @Test
    void repairsMissingProblemReadmeWhenSolutionAlreadyExists() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        String formattedCode = formatter.format(request);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                contains("solution.java"), eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode(formattedCode), "solution-sha", "solution.java"
        ));

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> path != null
                        && path.endsWith("/README.md")
                        && !path.equals("README.md")),
                eq(branch)
        )).thenReturn(null);

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("test-sha")
        ));

        SyncResponse response = service.sync(token, owner, request);

        assertEquals("SKIPPED", response.status());

        verify(githubClient, times(1)).putFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> path != null
                        && path.endsWith("/README.md")
                        && !path.equals("README.md")),
                any()
        );

        verify(githubClient, never()).putFile(
                eq(token), eq(owner), eq(repo),
                contains("solution.java"), any()
        );

        verify(githubClient, never()).putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), any()
        );
    }

    private SubmissionRequest request(
            String problemId,
            String title,
            String code
    ) {
        return new SubmissionRequest(
                SubmissionRequest.Source.LEETCODE,
                problemId,
                title,
                "Java",
                code,
                repo,
                null,
                null,
                null,
                null,
                null,
                true
        );
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }

    @Test
    void refusesToOverwriteRootReadmeWhenItCannotBeRead() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                contains("solution.java"), eq(branch)
        )).thenReturn(null);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> path != null
                        && path.endsWith("/README.md")
                        && !path.equals("README.md")),
                eq(branch)
        )).thenReturn(null);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), eq(branch)
        )).thenReturn(null);

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("test-sha")
        ));

        assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        verify(githubClient, never()).putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), any()
        );
    }
}