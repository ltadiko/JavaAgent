package com.jobagent.jobagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TODO: Sprint 1 — Remove the excludeName list below after configuring:
 *   - Spring Authorization Server (JWK keys, registered clients)
 *   - OAuth2 Resource Server (issuer-uri)
 *   - OAuth2 Client (provider registration)
 */
@SpringBootApplication
public class JavaAgentApplication {

    static void main(String[] args) {
        SpringApplication.run(JavaAgentApplication.class, args);
    }

}
