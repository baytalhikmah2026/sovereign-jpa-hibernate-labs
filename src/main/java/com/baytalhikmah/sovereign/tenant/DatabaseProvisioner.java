package com.baytalhikmah.sovereign.tenant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/** Opens a new shop with a database of its own on the same PostgreSQL server, and registers it. */
@Component
@Profile("tenancy")
@ConditionalOnProperty(name = "sovereign.tenancy.mode", havingValue = "database")
public class DatabaseProvisioner {

    private static final ClassPathResource CUSTOMER_TABLES = new ClassPathResource("db/tenant/customer-tables.sql");

    private final JdbcTemplate jdbcTemplate;

    private final JdbcConnectionDetails platform;

    private final TenantDatabaseRegistry registry;

    private final ShopSchemas names = new ShopSchemas();

    public DatabaseProvisioner(JdbcTemplate jdbcTemplate, JdbcConnectionDetails platform,
            TenantDatabaseRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.platform = platform;
        this.registry = registry;
    }

    public String provision(String shopCode) throws SQLException {
        String database = names.schemaName(shopCode);
        jdbcTemplate.execute("create database " + database);
        String jdbcUrl = withDatabase(platform.getJdbcUrl(), database);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, platform.getUsername(),
                platform.getPassword())) {
            ScriptUtils.executeSqlScript(connection, CUSTOMER_TABLES);
        }
        registry.register(shopCode, jdbcUrl, platform.getUsername(), platform.getPassword());
        System.out.println("Provisioned database " + database);
        return database;
    }

    /** jdbc:postgresql://host:5432/sovereign?x=y becomes jdbc:postgresql://host:5432/<database>?x=y */
    static String withDatabase(String jdbcUrl, String database) {
        int hostStart = jdbcUrl.indexOf("//") + 2;
        int pathStart = jdbcUrl.indexOf('/', hostStart);
        int queryStart = jdbcUrl.indexOf('?', pathStart);
        String query = queryStart < 0 ? "" : jdbcUrl.substring(queryStart);
        return jdbcUrl.substring(0, pathStart + 1) + database + query;
    }

}
