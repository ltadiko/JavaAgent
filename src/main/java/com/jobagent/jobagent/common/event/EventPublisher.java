package com.jobagent.jobagent.common.event;

import com.jobagent.jobagent.common.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publishes events to Kafka with tenant context in headers.
 * Every event is guaranteed to carry tenant_id.
 *
 * Conditional on KafkaTemplate — won't load if Kafka auto-config is not active.
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, BaseEvent event) {
        // Ensure tenant_id is set
        if (event.getTenantId() == null) {
            event.setTenantId(TenantContext.requireTenantId());
        }

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, event);
        // Add tenant_id as Kafka header for consumer-side filtering
        record.headers().add("tenant_id",
                event.getTenantId().toString().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to topic {}: {}",
                                event.getEventType(), topic, ex.getMessage());
                    } else {
                        log.debug("Published event {} to topic {} partition {} offset {}",
                                event.getEventType(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
