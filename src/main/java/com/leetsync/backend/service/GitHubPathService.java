package com.leetsync.backend.service;

import org.springframework.stereotype.Service;

@Service
public class GitHubPathService {

    public String generatePath(
            Integer problemNumber,
            String problemTitle,
            String language
    ) {
        String normalizedLanguage = normalizeLanguage(language);
        String slug = createSlug(problemTitle);
        String extension = getExtension(normalizedLanguage);

        return "solutions/"
                + normalizedLanguage
                + "/"
                + problemNumber
                + "-"
                + slug
                + extension;
    }

    private String normalizeLanguage(String language) {
        return switch (language.trim().toLowerCase()) {
            case "java" -> "java";
            case "c++", "cpp" -> "cpp";
            case "python", "python3" -> "python";
            case "javascript", "js" -> "javascript";
            case "typescript", "ts" -> "typescript";
            case "c" -> "c";
            case "c#" -> "csharp";
            case "go" -> "go";
            case "rust" -> "rust";
            case "kotlin" -> "kotlin";
            case "swift" -> "swift";
            default -> throw new IllegalArgumentException(
                    "Unsupported programming language: " + language
            );
        };
    }

    private String getExtension(String language) {
        return switch (language) {
            case "java" -> ".java";
            case "cpp" -> ".cpp";
            case "python" -> ".py";
            case "javascript" -> ".js";
            case "typescript" -> ".ts";
            case "c" -> ".c";
            case "csharp" -> ".cs";
            case "go" -> ".go";
            case "rust" -> ".rs";
            case "kotlin" -> ".kt";
            case "swift" -> ".swift";
            default -> throw new IllegalArgumentException(
                    "Unsupported programming language: " + language
            );
        };
    }

    private String createSlug(String title) {
        return title
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}