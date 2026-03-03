package com.jobagent.jobagent;

import com.jobagent.jobagent.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

/**
 * Base class for integration tests using Testcontainers.
 *
 * <p>Provides a PostgreSQL container that is shared across all tests in a class.
 * Sets up TenantContext before each test and clears it after.
 *
 * <p>Tests are automatically skipped if Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-containers")
@Testcontainers
@ExtendWith(DockerAvailableCondition.class)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("jobagent_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/init-pgvector.sql");

    protected UUID testTenantId;

    /**
     * Check if Docker is available for Testcontainers.
     */
    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (isDockerAvailable() && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        // Disable services not needed for integration tests
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:0");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "0");
        registry.add("spring.ai.ollama.enabled", () -> "false");
        // App settings for tests
        registry.add("app.encryption.secret-key", () -> "test-key-32-chars-for-aes-256!!");
    }

    @BeforeEach
    protected void setUpTenantContext() {
        testTenantId = UUID.randomUUID();
        TenantContext.setTenantId(testTenantId);
    }

    @AfterEach
    protected void clearTenantContext() {
        TenantContext.clear();
    }
}
