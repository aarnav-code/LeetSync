package com.leetsync.backend.service;

import com.leetsync.backend.dto.SubmissionRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CodeFileService {

    public String extension(String language) {
        String value = language.toLowerCase(Locale.ROOT).replace(" ", "");
        return switch (value) {
            case "cpp", "c++", "gnu++17", "gnu++20" -> "cpp";
            case "java" -> "java";
            case "python", "python3", "py" -> "py";
            case "javascript", "javascript/node", "js" -> "js";
            case "typescript", "ts" -> "ts";
            case "c" -> "c";
            case "csharp", "c#" -> "cs";
            case "kotlin" -> "kt";
            case "go", "golang" -> "go";
            case "rust" -> "rs";
            case "swift" -> "swift";
            case "php" -> "php";
            default -> "txt";
        };
    }

    public String sanitize(String value) {
        return value.trim()
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public String path(SubmissionRequest request) {
        String platform = switch (request.source()) {
            case LEETCODE -> "LeetCode";
            case GEEKS_FOR_GEEKS -> "GeeksForGeeks";
            case CODEFORCES -> "Codeforces";
        };
        return platform + "/" + sanitize(request.problemId()) + "-" + sanitize(request.problemTitle())
                + "/solution." + extension(request.language());
    }
}
