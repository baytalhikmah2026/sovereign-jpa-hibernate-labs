package com.baytalhikmah.sovereign.tenant;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** The master registry of shop databases, kept in the platform database (table sov_tenant_database). */
@Component
@Profile("tenancy")
@ConditionalOnProperty(name = "sovereign.tenancy.mode", havingValue = "database")
public class TenantDatabaseRegistry {

    /** How to reach one shop's database; the password is already decrypted. */
    public record Entry(String tenantId, String jdbcUrl, String username, String password) {

        @Override
        public String toString() {
            return "Entry[tenantId=" + tenantId + ", jdbcUrl=" + jdbcUrl + ", username=" + username + ", password=***]";
        }

    }

    private final JdbcTemplate jdbcTemplate;

    private final SecretBox secretBox;

    public TenantDatabaseRegistry(JdbcTemplate jdbcTemplate, SecretBox secretBox) {
        this.jdbcTemplate = jdbcTemplate;
        this.secretBox = secretBox;
    }

    public void register(String tenantId, String jdbcUrl, String username, String password) {
        jdbcTemplate.update("""
                insert into sov_tenant_database (tenant_id, jdbc_url, username, password_ciphertext, registered_at)
                values (?, ?, ?, ?, ?)""",
            tenantId, jdbcUrl, username, secretBox.encrypt(password), Timestamp.from(Instant.now()));
    }

    public Optional<Entry> find(String tenantId) {
        return jdbcTemplate.query("""
                select jdbc_url, username, password_ciphertext from sov_tenant_database
                where tenant_id = ?""",
            (row, number) -> new Entry(tenantId, row.getString("jdbc_url"), row.getString("username"),
                secretBox.decrypt(row.getString("password_ciphertext"))),
            tenantId)
            .stream()
            .findFirst();
    }

}
