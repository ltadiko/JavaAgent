package com.jobagent.jobagent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context loading test.
 * Requires Docker for database connection.
 */
@SpringBootTest
@ActiveProfiles("test-containers")
@ExtendWith(DockerAvailableCondition.class)
class JavaAgentApplicationTests {


    @Test
    void contextLoads() {
    }

}
