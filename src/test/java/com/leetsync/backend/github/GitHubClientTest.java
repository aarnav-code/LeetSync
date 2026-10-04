
package com.leetsync.backend.github;

import com.leetsync.backend.dto.GitHubContentsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import com.leetsync.backend.exception.ApiException;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GitHubClientTest {

    private AtomicReference<ClientRequest> capturedRequest;
    private GitHubClient githubClient;

    @BeforeEach
    void setUp() {
        capturedRequest = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);

            return Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                            .header(
                                    "Content-Type",
                                    MediaType.APPLICATION_JSON_VALUE
                            )
                            .body("""
                                    {
                                      "content": "IyBMZWV0U3luYw==",
                                      "sha": "test-sha",
                                      "path": "README.md"
                                    }
                                    """)
                            .build()
            );
        };

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .exchangeFunction(exchangeFunction)
                .build();

        githubClient = new GitHubClient(webClient);
    }

    @Test
    void getFileBuildsCorrectRootReadmeUrl() {
        GitHubContentsResponse response = githubClient.getFile(
                "test-token",
                "test-owner",
                "test-repo",
                "README.md",
                "main"
        );

        assertNotNull(response);
        assertEquals("README.md", response.path());

        ClientRequest request = capturedRequest.get();

        assertEquals(
                "/repos/test-owner/test-repo/contents/README.md?ref=main",
                request.url().getRawPath() + "?" + request.url().getRawQuery()
        );

        assertEquals(
                "Bearer test-token",
                request.headers().getFirst("Authorization")
        );
    }

    @Test
    void getFileBuildsCorrectNestedReadmeUrl() {
        githubClient.getFile(
                "test-token",
                "test-owner",
                "test-repo",
                "0001-two-sum/README.md",
                "main"
        );

        ClientRequest request = capturedRequest.get();

        assertEquals(
                "/repos/test-owner/test-repo/contents/0001-two-sum/README.md?ref=main",
                request.url().getRawPath() + "?" + request.url().getRawQuery()
        );
    }


    @Test
    void getFileReturnsNullWhenFileDoesNotExist() {
        ExchangeFunction exchangeFunction = request ->
                Mono.just(
                        ClientResponse.create(HttpStatus.NOT_FOUND)
                                .build()
                );

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .exchangeFunction(exchangeFunction)
                .build();

        GitHubClient client = new GitHubClient(webClient);

        GitHubContentsResponse response = client.getFile(
                "test-token",
                "test-owner",
                "test-repo",
                "missing.java",
                "main"
        );

        assertNull(response);
    }

    @Test
    void getFileThrowsApiExceptionWhenGitHubReturnsServerError() {
        ExchangeFunction exchangeFunction = request ->
                Mono.just(
                        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                                .build()
                );

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .exchangeFunction(exchangeFunction)
                .build();

        GitHubClient client = new GitHubClient(webClient);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> client.getFile(
                        "test-token",
                        "test-owner",
                        "test-repo",
                        "README.md",
                        "main"
                )
        );

        assertEquals(
                "GITHUB_FILE_READ_ERROR",
                exception.getCode()
        );
    }
}