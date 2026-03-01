package com.jobagent.jobagent.common.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Sprint 10.3 — Rate limiting configuration using Resilience4j.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Auth endpoints: 10 requests per minute.
     */
    @Bean
    public RateLimiter authRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        return registry.rateLimiter("auth", config);
    }

    /**
     * AI endpoints: 5 requests per minute.
     */
    @Bean
    public RateLimiter aiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        return registry.rateLimiter("ai", config);
    }

    /**
     * Search endpoints: 30 requests per minute.
     */
    @Bean
    public RateLimiter searchRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(30)
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        return registry.rateLimiter("search", config);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }
}
