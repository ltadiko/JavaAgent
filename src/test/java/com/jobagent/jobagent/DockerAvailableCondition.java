package com.jobagent.jobagent;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 extension to skip tests when Docker is not available.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static Boolean dockerAvailable;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (dockerAvailable == null) {
            dockerAvailable = checkDockerAvailable();
        }

        if (dockerAvailable) {
            return ConditionEvaluationResult.enabled("Docker is available");
        } else {
            return ConditionEvaluationResult.disabled("Docker is not available - skipping integration test");
        }
    }

    private boolean checkDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
