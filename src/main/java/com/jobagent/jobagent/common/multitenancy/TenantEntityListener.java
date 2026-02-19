package com.jobagent.jobagent.common.multitenancy;

import com.jobagent.jobagent.common.model.BaseEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.UUID;

/**
 * JPA entity listener that enforces tenant_id consistency on every write.
 * Prevents a compromised JWT from writing to another tenant's data.
 * (Gap #1 from design audit)
 */
public class TenantEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            UUID currentTenant = TenantContext.requireTenantId();

            if (baseEntity.getTenantId() == null) {
                // Auto-set tenant_id from context
                baseEntity.setTenantId(currentTenant);
            } else if (!baseEntity.getTenantId().equals(currentTenant)) {
                throw new SecurityException(
                        "Tenant mismatch: entity tenant_id=" + baseEntity.getTenantId()
                                + " does not match current tenant=" + currentTenant);
            }
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            UUID currentTenant = TenantContext.requireTenantId();
            if (!baseEntity.getTenantId().equals(currentTenant)) {
                throw new SecurityException(
                        "Tenant mismatch on update: entity tenant_id=" + baseEntity.getTenantId()
                                + " does not match current tenant=" + currentTenant);
            }
        }
    }
}
