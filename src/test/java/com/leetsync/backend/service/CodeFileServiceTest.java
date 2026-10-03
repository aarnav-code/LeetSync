package com.leetsync.backend.service;

import com.leetsync.backend.dto.SubmissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeFileServiceTest {
    private final CodeFileService service = new CodeFileService();

    @Test
    void createsStableLeetCodePath() {
        SubmissionRequest request = new SubmissionRequest(
                SubmissionRequest.Source.LEETCODE,
                "1",
                "Two Sum",
                "Java",
                "class Solution {}",
                "dsa-solutions",
                "main",
                "3 ms",
                98.2,
                "45 MB",
                80.1,
                true
        );

        assertEquals("0001-two-sum/solution.java", service.path(request));
    }
}
