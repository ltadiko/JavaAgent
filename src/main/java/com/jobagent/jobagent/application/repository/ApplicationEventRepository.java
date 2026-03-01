package com.jobagent.jobagent.application.repository;

import com.jobagent.jobagent.application.model.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Sprint 8.4 — Repository for ApplicationEvent entity.
 */
@Repository
public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {

    /**
     * Find all events for an application, ordered newest-first.
     */
    List<ApplicationEvent> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    /**
     * Find all events for an application within a tenant.
     */
    List<ApplicationEvent> findByApplicationIdAndTenantIdOrderByCreatedAtDesc(
            UUID applicationId, UUID tenantId);
}
