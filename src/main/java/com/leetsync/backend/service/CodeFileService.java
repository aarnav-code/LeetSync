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
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    public String problemDirectory(SubmissionRequest request) {
        String number = request.problemId().replaceAll("\\D", "");

        if (number.isBlank()) {
            throw new IllegalArgumentException("Problem number is missing or invalid.");
        }

        String paddedNumber = String.format("%04d", Integer.parseInt(number));
        String slug = sanitize(request.problemTitle());

        return paddedNumber + "-" + slug;
    }

    public String path(SubmissionRequest request) {
        return problemDirectory(request)
                + "/solution."
                + extension(request.language());
    }

    public String problemReadmePath(SubmissionRequest request) {
        return problemDirectory(request) + "/README.md";
    }
}
