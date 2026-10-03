package com.leetsync.backend.service;

import org.springframework.stereotype.Service;

@Service
public class GitHubPathService {

    public String generatePath(
            Integer problemNumber,
            String problemTitle,
            String language
    ) {
        String slug = createSlug(problemTitle);
        String extension = getExtension(language);

        String directory = String.format(
                "%04d-%s",
                problemNumber,
                slug
        );

        return directory + "/solution" + extension;
    }

    public String generateProblemReadmePath(
            Integer problemNumber,
            String problemTitle
    ) {
        String slug = createSlug(problemTitle);

        String directory = String.format(
                "%04d-%s",
                problemNumber,
                slug
        );

        return directory + "/README.md";
    }

    private String getExtension(String language) {
        return switch (language.trim().toLowerCase()) {
            case "java" -> ".java";
            case "c++", "cpp" -> ".cpp";
            case "python", "python3" -> ".py";
            case "javascript", "js" -> ".js";
            case "typescript", "ts" -> ".ts";
            case "c" -> ".c";
            case "c#" -> ".cs";
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