package com.leetsync.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public AppConfig(@Value("${app.cors.allowed-origin-patterns}") String patterns) {
        this.allowedOriginPatterns = patterns.split(",");
    }

    @Bean
    WebClient githubWebClient(
            WebClient.Builder builder,
            @Value("${app.github.api-base-url}") String baseUrl,
            @Value("${app.github.user-agent}") String userAgent) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "OPTIONS", "DELETE", "PUT")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
