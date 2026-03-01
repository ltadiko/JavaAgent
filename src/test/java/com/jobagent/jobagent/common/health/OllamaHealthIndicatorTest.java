package com.jobagent.jobagent.common.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10.6 — Unit tests for OllamaHealthIndicator.
 */
@DisplayName("OllamaHealthIndicator Tests")
class OllamaHealthIndicatorTest {

    @Test
    @DisplayName("Reports DOWN when Ollama is unreachable")
    void health_unreachable_returnsDown() {
        OllamaHealthIndicator indicator = new OllamaHealthIndicator("http://localhost:99999");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsEntry("url", "http://localhost:99999");
    }
}
