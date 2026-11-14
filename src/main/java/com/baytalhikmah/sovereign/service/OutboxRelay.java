package com.baytalhikmah.sovereign.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.OutboxEvent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The postman: regularly picks up waiting outbox events, sends each one to Kafka and marks it published.
 * "for update skip locked" lets several copies of the application share the work without sending twice.
 */
@Component
@Profile("outbox-relay")
public class OutboxRelay {

    private static final String PENDING = """
        select * from sov_outbox_event
        where published_at is null
        order by occurred_at
        limit 50
        for update skip locked
        """;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public OutboxRelay(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public static String topicFor(String aggregateType) {
        return "outbox.event." + aggregateType;
    }

    /** Sends up to 50 waiting events; returns how many were sent. */
    @Scheduled(fixedDelayString = "${sovereign.outbox.poll-interval-ms:500}")
    @Transactional
    public int publishPending() {
        @SuppressWarnings("unchecked")
        List<OutboxEvent> pending = entityManager.createNativeQuery(PENDING, OutboxEvent.class).getResultList();
        for (OutboxEvent event : pending) {
            ProducerRecord<String, String> message = new ProducerRecord<>(topicFor(event.getAggregateType()),
                event.getAggregateId(), event.getPayload());
            message.headers().add("id", event.getId().toString().getBytes(StandardCharsets.UTF_8));
            message.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(message).join();
            event.markPublished(Instant.now());
        }
        return pending.size();
    }

}
