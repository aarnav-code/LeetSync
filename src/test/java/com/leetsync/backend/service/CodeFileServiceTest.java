package com.leetsync.backend.service;

import com.leetsync.backend.dto.SubmissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeFileServiceTest {
    private final CodeFileService service = new CodeFileService();

    @Test
    void createsStableLeetCodePath() {
        SubmissionRequest request = createRequest(
                SubmissionRequest.Source.LEETCODE,
                "1",
                "Two Sum",
                "Java"
        );

        assertEquals(
                "LeetCode Solutions/0001-two-sum/solution.java",
                service.path(request)
        );
    }

    @Test
    void createsStableCodeforcesPath() {
        SubmissionRequest request = createRequest(
                SubmissionRequest.Source.CODEFORCES,
                "4A",
                "Watermelon",
                "GNU C++17"
        );

        assertEquals(
                "Codeforces Solutions/0004-watermelon/solution.cpp",
                service.path(request)
        );
    }

    @Test
    void createsStableGeeksForGeeksPath() {
        SubmissionRequest request = createRequest(
                SubmissionRequest.Source.GEEKS_FOR_GEEKS,
                "42",
                "Reverse Array",
                "Python"
        );

        assertEquals(
                "GeeksforGeeks Solutions/0042-reverse-array/solution.py",
                service.path(request)
        );
    }

    private SubmissionRequest createRequest(
            SubmissionRequest.Source source,
            String problemId,
            String problemTitle,
            String language
    ) {
        return new SubmissionRequest(
                source,
                problemId,
                problemTitle,
                language,
                "sample code",
                "dsa-solutions",
                "main",
                "3 ms",
                98.2,
                "45 MB",
                80.1,
                true
        );
    }
}
