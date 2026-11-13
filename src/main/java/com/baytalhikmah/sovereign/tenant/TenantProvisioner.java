package com.baytalhikmah.sovereign.tenant;

import java.sql.Statement;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/** Opens a new shop: creates its schema and the customer tables inside it. */
@Component
@Profile("tenancy")
public class TenantProvisioner {

    private static final ClassPathResource CUSTOMER_TABLES = new ClassPathResource("db/tenant/customer-tables.sql");

    private final JdbcTemplate jdbcTemplate;

    private final ShopSchemas schemas = new ShopSchemas();

    public TenantProvisioner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String provision(String shopCode) {
        String schema = schemas.schemaName(shopCode);
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("create schema if not exists " + schema);
            }
            String previous = connection.getSchema();
            connection.setSchema(schema);
            try {
                ScriptUtils.executeSqlScript(connection, CUSTOMER_TABLES);
            }
            finally {
                connection.setSchema(previous);
            }
            return null;
        });
        System.out.println("Provisioned schema " + schema);
        return schema;
    }

}
