package com.jobagent.jobagent.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all Kafka events. Every event carries tenant context.
 * (Gap #2 from design audit — Kafka events must carry tenant_id)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    private UUID eventId;
    private UUID tenantId;
    private String eventType;
    private Instant timestamp;

    protected BaseEvent(UUID tenantId, String eventType) {
        this.eventId = UUID.randomUUID();
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }
}
