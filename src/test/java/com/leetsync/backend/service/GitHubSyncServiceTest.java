package com.leetsync.backend.service;

import com.leetsync.backend.dto.*;
import com.leetsync.backend.exception.ApiException;
import com.leetsync.backend.github.GitHubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

        lenient().when(githubClient.getRepository(token, owner, repo))
                .thenReturn(new GitHubRepositoryResponse(
                        repo, branch, false, false, "private", null
                ));
    }

    @Test
    void createsNewProblemAndUpdatesGlobalStats() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode("# LeetSync\n\nMy project documentation.\n"),
                "root-readme-sha",
                "README.md"
        );

        mockSuccessfulWrites();

        SyncResponse response = service.sync(token, owner, request);

        assertEquals("CREATED", response.status());

        verify(githubClient, times(3)).putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        );

        verify(githubClient, times(1)).getFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), eq(branch)
        );

        verify(githubClient).putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"),
                argThat(body -> {
                    String updated = decode(body.content());
                    return updated.contains("My project documentation.")
                            && updated.contains(
                            "Global stats: 1 Problem Solved"
                    );
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
                encode("# 1. Two Sum"),
                "readme-sha",
                "0001-two-sum/README.md"
        ));

        mockSuccessfulWrites();

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
                encode(formattedCode),
                "solution-sha",
                "solution.java"
        ));

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> path != null
                        && path.endsWith("/README.md")
                        && !path.equals("README.md")),
                eq(branch)
        )).thenReturn(null);

        mockSuccessfulWrites();

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

    @Test
    void refusesToOverwriteRootReadmeWhenItCannotBeRead() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        mockMissingSolutionAndProblemReadme();
        mockRootReadme(null, null, null);

        assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        verifyNoWrites();
    }

    @Test
    void preservesRootReadmeDocumentationAndDirectoryTreeWhenUpdatingStats() {
        SubmissionRequest request =
                request("2", "Add Two Numbers", "class Solution {}");

        String originalRootReadme = """
                # LeetSync

                Automatically sync coding solutions to GitHub.

                ## Directory Structure

                ```text
                LeetCode Solutions/
                ├── 0001-two-sum/
                │   ├── solution.java
                │   └── README.md
                └── README.md
                ```

                Global stats: 1 Problem Solved

                ## About

                This project tracks my coding progress.
                """;

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode(originalRootReadme),
                "root-readme-sha",
                "README.md"
        );

        mockSuccessfulWrites();

        SyncResponse response = service.sync(token, owner, request);

        assertEquals("CREATED", response.status());

        String expectedRootReadme = originalRootReadme.replace(
                "Global stats: 1 Problem Solved",
                "Global stats: 2 Problems Solved"
        ).stripTrailing()
                + "\n\n"
                + "<!-- LEETSYNC:COUNTED:LEETCODE:2 -->"
                + "\n";

        verify(githubClient).putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"),
                argThat(body -> expectedRootReadme.equals(
                        decode(body.content())
                ))
        );
    }

    @Test
    void refusesToWriteWhenRootReadmeHasNoSha() {
        SubmissionRequest request =
                request("2", "Add Two Numbers", "class Solution {}");

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode("# LeetSync\n\nGlobal stats: 1 Problem Solved\n"),
                null,
                "README.md"
        );

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        assertEquals("ROOT_README_READ_ERROR", exception.getCode());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());

        verifyNoWrites();
    }

    @Test
    void refusesToWriteWhenRootReadmePathIsUnexpected() {
        SubmissionRequest request =
                request("3", "Longest Substring", "class Solution {}");

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode("# Some other file"),
                "some-file-sha",
                "docs/README.md"
        );

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        assertEquals("ROOT_README_READ_ERROR", exception.getCode());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());

        verifyNoWrites();
    }

    @Test
    void refusesToWriteWhenRootReadmeContentIsInvalidBase64() {
        SubmissionRequest request =
                request("4", "Median of Two Sorted Arrays", "class Solution {}");

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                "not-valid-base64!!!",
                "root-readme-sha",
                "README.md"
        );

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        assertEquals("ROOT_README_READ_ERROR", exception.getCode());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());

        verifyNoWrites();
    }

    private void mockMissingSolutionAndProblemReadme() {
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
    }

    private void mockRootReadme(
            String content,
            String sha,
            String path
    ) {
        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), eq(branch)
        )).thenReturn(
                content == null && sha == null && path == null
                        ? null
                        : new GitHubContentsResponse(content, sha, path)
        );
    }

    private void mockSuccessfulWrites() {
        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("test-sha")
        ));
    }

    private void verifyNoWrites() {
        verify(githubClient, never()).putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
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
    void propagatesGitHubWriteFailure() {
        SubmissionRequest request =
                request("5", "Test Problem", "class Solution {}");

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode("# LeetSync\n\nGlobal stats: 4 Problems Solved\n"),
                "root-readme-sha",
                "README.md"
        );

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo),
                anyString(), any()
        )).thenThrow(
                WebClientResponseException.create(
                        502,
                        "Bad Gateway",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )
        );

        assertThrows(
                WebClientResponseException.class,
                () -> service.sync(token, owner, request)
        );
    }

    @Test
    void skipsCreatingDuplicateWhenLegacySolutionAlreadyExists() {
        SubmissionRequest request =
                request("1", "Two Sum", "class Solution {}");

        String legacyPath = "0001-two-sum/solution.java";

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                contains("solution.java"), eq(branch)
        )).thenReturn(null);

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                argThat(path -> legacyPath.equals(path)),
                eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode("class Solution {}"),
                "legacy-solution-sha",
                legacyPath
        ));

        SyncResponse response = service.sync(token, owner, request);

        assertEquals("SKIPPED", response.status());
        assertEquals(legacyPath, response.path());

        verify(githubClient, never()).putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        );
    }

    @Test
    void retriesAfterRootReadmeWriteFails() {
        SubmissionRequest request =
                request("6", "Retry Test Problem", "class Solution {}");

        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode("# LeetSync\n\nGlobal stats: 5 Problems Solved\n"),
                "root-readme-sha",
                "README.md"
        );

        doAnswer(invocation -> {
            String path = invocation.getArgument(3);

            if ("README.md".equals(path)) {
                throw WebClientResponseException.create(
                        502,
                        "Bad Gateway",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                );
            }

            return new GitHubPutFileResponse(
                    new GitHubPutFileResponse.Commit("test-sha")
            );
        }).when(githubClient).putFile(
                eq(token), eq(owner), eq(repo), anyString(), any()
        );

        assertThrows(
                WebClientResponseException.class,
                () -> service.sync(token, owner, request)
        );
    }

    @Test
    void rejectsSubmissionWhenAcceptedIsFalse() {
        SubmissionRequest request =
                new SubmissionRequest(
                        SubmissionRequest.Source.LEETCODE,
                        "1",
                        "Two Sum",
                        "Java",
                        "class Solution {}",
                        repo,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false
                );

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        assertEquals("NOT_ACCEPTED", exception.getCode());
        verifyNoWrites();
    }

    @Test
    void rejectsSubmissionWhenAcceptedIsNull() {
        SubmissionRequest request =
                new SubmissionRequest(
                        SubmissionRequest.Source.LEETCODE,
                        "1",
                        "Two Sum",
                        "Java",
                        "class Solution {}",
                        repo,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.sync(token, owner, request)
        );

        assertEquals("NOT_ACCEPTED", exception.getCode());
        verifyNoWrites();
    }

    @Test
    void retriesRootReadmeUpdateAfterPartialSyncFailure() {
        SubmissionRequest request =
                request("7", "Retry Recovery Test", "class Solution {}");

        String solutionPath =
                codeFileService.path(request);

        String problemReadmePath =
                codeFileService.problemReadmePath(request);

        String originalRootContent =
                "# LeetSync\n\nGlobal stats: 5 Problems Solved\n";

        // First attempt: solution and problem README do not exist.
        mockMissingSolutionAndProblemReadme();

        mockRootReadme(
                encode(originalRootContent),
                "root-readme-sha",
                "README.md"
        );

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo),
                eq(solutionPath), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("solution-sha")
        ));

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo),
                eq(problemReadmePath), any()
        )).thenReturn(new GitHubPutFileResponse(
                new GitHubPutFileResponse.Commit("problem-readme-sha")
        ));

        when(githubClient.putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), any()
        )).thenThrow(
                WebClientResponseException.create(
                        502,
                        "Bad Gateway",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )
        );

        assertThrows(
                WebClientResponseException.class,
                () -> service.sync(token, owner, request)
        );

        // Retry: the solution and problem README now exist.
        reset(githubClient);

        when(githubClient.getRepository(token, owner, repo))
                .thenReturn(new GitHubRepositoryResponse(
                        repo, branch, false, false, "private", null
                ));

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                eq(solutionPath), eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode(formatter.format(request)),
                "solution-sha",
                solutionPath
        ));

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                eq(problemReadmePath), eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode(
                        "# 7. Retry Recovery Test\n\n"
                                + "- Source: LEETCODE\n"
                                + "- Language: Java\n\n"
                                + "<!-- LEETSYNC:PENDING:LEETCODE:7 -->\n"
                ),
                "problem-readme-sha",
                problemReadmePath
        ));

        when(githubClient.getFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), eq(branch)
        )).thenReturn(new GitHubContentsResponse(
                encode(originalRootContent),
                "root-readme-sha",
                "README.md"
        ));

        SyncResponse retryResponse =
                service.sync(token, owner, request);

        assertEquals("SKIPPED", retryResponse.status());

        verify(githubClient, times(1)).putFile(
                eq(token), eq(owner), eq(repo),
                eq("README.md"), any()
        );
    }
}