package com.jobagent.jobagent.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sprint 10.2 — Ollama AI service health indicator.
 */
@Component("ollama")
@Slf4j
public class OllamaHealthIndicator implements HealthIndicator {

    private final String ollamaBaseUrl;
    private final RestClient restClient;

    public OllamaHealthIndicator(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.restClient = RestClient.builder().baseUrl(ollamaBaseUrl).build();
    }

    @Override
    public Health health() {
        try {
            String response = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);

            return Health.up()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            log.debug("Ollama health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
