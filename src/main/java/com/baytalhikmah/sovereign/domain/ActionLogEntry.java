package com.baytalhikmah.sovereign.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One line of the flight recorder: who tried to do what, kept even when the attempt itself fails. */
@Entity
public class ActionLogEntry {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String actor;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String action;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String detail;

    @Column(nullable = false)
    private Instant recordedAt;

    protected ActionLogEntry() {
    }

    public ActionLogEntry(String actor, String action, String detail) {
        this.actor = actor;
        this.action = action;
        this.detail = detail;
        this.recordedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

}
