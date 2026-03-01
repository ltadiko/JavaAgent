package com.jobagent.jobagent.test;

import com.jobagent.jobagent.common.multitenancy.TenantContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.UUID;

public class TestTenantExecutionListener implements TestExecutionListener {

    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        // Set a default tenant for tests when TenantContext isn't provided by the filter
        TenantContext.setTenantId(DEFAULT_TENANT);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        TenantContext.clear();
    }

}
