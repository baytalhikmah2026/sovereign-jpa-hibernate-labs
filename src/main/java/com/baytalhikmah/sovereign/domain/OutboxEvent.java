package com.baytalhikmah.sovereign.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A message waiting to leave the shop, saved in the same transaction as the business change it describes.
 * aggregateType says what kind of thing changed ("order"), aggregateId which one ("Q77-0001").
 */
@Entity
@Table(indexes = @Index(name = "sov_outbox_event_pending", columnList = "published_at, occurred_at"))
public class OutboxEvent {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 40, updatable = false)
    private String aggregateType;

    @Column(nullable = false, length = 64, updatable = false)
    private String aggregateId;

    @Column(nullable = false, length = 60, updatable = false)
    private String eventType;

    @ColumnTransformer(write = "?::jsonb")
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = Instant.now();
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

}
