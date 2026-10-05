package com.leetsync.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class GitHubSolutionRequest {

    @NotNull
    @Positive
    private Integer problemNumber;

    @NotBlank
    private String problemTitle;

    @NotBlank
    private String language;

    @NotBlank
    private String code;

    @NotNull
    private Boolean accepted;

    public Integer getProblemNumber() {
        return problemNumber;
    }

    public void setProblemNumber(Integer problemNumber) {
        this.problemNumber = problemNumber;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }
}