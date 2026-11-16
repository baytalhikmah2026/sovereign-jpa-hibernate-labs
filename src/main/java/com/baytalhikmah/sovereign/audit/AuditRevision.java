package com.baytalhikmah.sovereign.audit;

import java.time.Instant;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row per Envers revision (one committed transaction that changed audited data): when it happened,
 * who did it, from which address, and the trace ID that links it to the request's trace in Jaeger.
 */
@Entity
@Table(name = "audit_revision")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision {

    @Id
    @GeneratedValue
    @RevisionNumber
    private long id;

    @RevisionTimestamp
    @Column(name = "changed_at_millis", nullable = false)
    private long changedAtMillis;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(length = 45)
    private String clientIp;

    @Column(length = 32)
    private String traceId;

    public long getId() {
        return id;
    }

    public Instant getChangedAt() {
        return Instant.ofEpochMilli(changedAtMillis);
    }

    public String getUsername() {
        return username;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getTraceId() {
        return traceId;
    }

    void recordIdentity(String username, String clientIp, String traceId) {
        this.username = username;
        this.clientIp = clientIp;
        this.traceId = traceId;
    }

}
