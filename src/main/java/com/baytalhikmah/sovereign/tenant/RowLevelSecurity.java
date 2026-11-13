package com.baytalhikmah.sovereign.tenant;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.hibernate.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.baytalhikmah.sovereign.domain.MarketplaceListing;

import jakarta.persistence.EntityManager;

/**
 * Two independent guards for the shared marketplace table: a Hibernate filter on every JPQL query,
 * and a PostgreSQL row-level security policy that the database applies to every statement.
 */
@Component
public class RowLevelSecurity {

    static final String TENANT_ROLE = "sovereign_tenant";

    private static final List<String> POLICY = List.of(
        "grant select, insert, update, delete on sov_marketplace_listing to " + TENANT_ROLE,
        "grant " + TENANT_ROLE + " to current_user",
        "alter table sov_marketplace_listing enable row level security",
        "alter table sov_marketplace_listing force row level security",
        "drop policy if exists tenant_isolation on sov_marketplace_listing",
        """
        create policy tenant_isolation on sov_marketplace_listing
            using (tenant_id = current_setting('sovereign.tenant_id', true))
            with check (tenant_id = current_setting('sovereign.tenant_id', true))""");

    private final JdbcTemplate jdbcTemplate;

    public RowLevelSecurity(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Creates the role and the policy. Run once, by the database owner (an administrator). */
    public void install() {
        Integer roles = jdbcTemplate.queryForObject("select count(*) from pg_roles where rolname = ?", Integer.class,
            TENANT_ROLE);
        if (roles == 0) {
            jdbcTemplate.execute("create role " + TENANT_ROLE + " nologin");
        }
        POLICY.forEach(jdbcTemplate::execute);
    }

    /**
     * Puts the current transaction under the current tenant: turns the Hibernate filter on, tells PostgreSQL
     * the tenant, and drops to the restricted role until the transaction ends.
     */
    public void enterTenant(EntityManager entityManager) {
        String tenant = TenantContext.get();
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(MarketplaceListing.TENANT_FILTER).setParameter("tenantId", tenant);
        session.doWork(connection -> {
            try (PreparedStatement setTenant = connection.prepareStatement(
                    "select set_config('sovereign.tenant_id', ?, true)")) {
                setTenant.setString(1, tenant);
                setTenant.execute();
            }
            try (Statement dropPrivileges = connection.createStatement()) {
                dropPrivileges.execute("set local role " + TENANT_ROLE);
            }
        });
    }

}
