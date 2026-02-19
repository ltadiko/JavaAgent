package com.jobagent.jobagent.common.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Simple root endpoint — confirms the API is running.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "JobAgent",
                "status", "UP",
                "timestamp", Instant.now()
        );
    }
}
