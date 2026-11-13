package com.baytalhikmah.sovereign.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * One row of the master tenant registry: where a shop's own database is and how to log in to it.
 * The password is only ever stored encrypted. The router reads this table with plain JDBC.
 */
@Entity
public class TenantDatabase {

    @Id
    @Column(length = 21)
    private String tenantId;

    @Column(nullable = false, length = 300)
    private String jdbcUrl;

    @Column(nullable = false, length = 63)
    private String username;

    @Column(nullable = false, length = 500)
    private String passwordCiphertext;

    @Column(nullable = false)
    private Instant registeredAt;

    protected TenantDatabase() {
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordCiphertext() {
        return passwordCiphertext;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

}
