package com.leetsync.backend.service;

import com.leetsync.backend.dto.SubmissionRequest;
import org.springframework.stereotype.Component;

@Component
public class SubmissionFormatter {

    public String format(SubmissionRequest request) {
        String code = request.code().replace("\r\n", "\n");

        String header = "// LeetSync metadata\n"
                + "// Source: " + request.source() + "\n"
                + "// Problem: " + request.problemId() + ". " + request.problemTitle() + "\n"
                + "// Language: " + request.language() + "\n";

        if (request.runtime() != null || request.runtimePercentile() != null) {
            header += "// Runtime: " + safe(request.runtime())
                    + percentile(request.runtimePercentile()) + "\n";
        }

        if (request.memory() != null || request.memoryPercentile() != null) {
            header += "// Memory: " + safe(request.memory())
                    + percentile(request.memoryPercentile()) + "\n";
        }

        return header + "\n" + code + (code.endsWith("\n") ? "" : "\n");
    }

    public String extractCode(String formattedFile) {
        String normalized = formattedFile.replace("\r\n", "\n");

        int separator = normalized.indexOf("\n\n");

        if (separator == -1) {
            // Not a LeetSync-formatted file.
            // Treat the entire file as source code.
            return normalized.trim();
        }

        return normalized.substring(separator + 2).trim();
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String percentile(Double value) {
        return value == null ? "" : " | Beats " + value + "%";
    }
}