package com.leetsync.backend.service;

import com.leetsync.backend.dto.SubmissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionFormatterTest {
    @Test
    void addsPerformanceMetadata() {
        SubmissionRequest request = new SubmissionRequest(
                SubmissionRequest.Source.LEETCODE,
                "1", "Two Sum", "Java", "class Solution {}", "repo", null,
                "3 ms", 98.0, "45 MB", 80.0, true
        );

        String output = new SubmissionFormatter().format(request);
        assertTrue(output.contains("Runtime: 3 ms | Beats 98.0%"));
        assertTrue(output.contains("Memory: 45 MB | Beats 80.0%"));
        assertTrue(output.contains("class Solution {}"));
    }
}
