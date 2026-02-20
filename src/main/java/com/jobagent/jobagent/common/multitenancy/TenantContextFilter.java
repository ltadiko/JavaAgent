package com.jobagent.jobagent.common.multitenancy;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Extracts tenant_id from the JWT and:
 *   1. Sets TenantContext (thread-local)
 *   2. Sets PostgreSQL session variable for RLS: SET LOCAL app.current_tenant = '...'
 *   3. Adds tenantId + userId to MDC for structured logging
 *
 * Only registered when a DataSource bean is available (avoids breaking @WebMvcTest slices).
 */
@Component
@ConditionalOnBean(DataSource.class)
@Order(1)
@Slf4j
public class TenantContextFilter implements Filter {

    private final DataSource dataSource;

    public TenantContextFilter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String tenantIdStr = jwt.getClaimAsString("tenant_id");
                String userId = jwt.getSubject();

                if (tenantIdStr != null) {
                    UUID tenantId = UUID.fromString(tenantIdStr);
                    TenantContext.setTenantId(tenantId);

                    // MDC for structured logging
                    MDC.put("tenantId", tenantIdStr);
                    MDC.put("userId", userId != null ? userId : "");

                    // Set PostgreSQL session variable for RLS
                    setPostgresTenantContext(tenantIdStr);
                }
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }

    private void setPostgresTenantContext(String tenantId) {
        try (Connection conn = dataSource.getConnection()) {
            try (var stmt = conn.prepareStatement("SET LOCAL app.current_tenant = ?")) {
                stmt.setString(1, tenantId);
                stmt.execute();
            }
        } catch (SQLException e) {
            log.warn("Failed to set PostgreSQL tenant context: {}", e.getMessage());
        }
    }
}
