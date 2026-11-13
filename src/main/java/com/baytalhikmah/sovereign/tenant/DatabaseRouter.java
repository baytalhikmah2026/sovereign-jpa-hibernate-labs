package com.baytalhikmah.sovereign.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Stoppable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Routes every Hibernate session to the database of its tenant. The platform uses the application's
 * own DataSource; each shop gets a small connection pool, opened the first time the shop is used.
 * A tenant that is not in the registry gets no connection at all.
 */
@Component
@Profile("tenancy")
@ConditionalOnProperty(name = "sovereign.tenancy.mode", havingValue = "database")
public class DatabaseRouter implements MultiTenantConnectionProvider<String>, Stoppable {

    private final DataSource platformDataSource;

    private final TenantDatabaseRegistry registry;

    private final Map<String, HikariDataSource> shopPools = new ConcurrentHashMap<>();

    public DatabaseRouter(DataSource platformDataSource, TenantDatabaseRegistry registry) {
        this.platformDataSource = platformDataSource;
        this.registry = registry;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return platformDataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenant) throws SQLException {
        if (TenantContext.PLATFORM.equals(tenant)) {
            return platformDataSource.getConnection();
        }
        return shopPools.computeIfAbsent(tenant, this::openPool).getConnection();
    }

    @Override
    public void releaseConnection(String tenant, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /** The shops whose pools are open, in alphabetical order. */
    public List<String> openPools() {
        return shopPools.keySet().stream().sorted().toList();
    }

    private HikariDataSource openPool(String tenant) {
        TenantDatabaseRegistry.Entry entry = registry.find(tenant)
            .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + tenant));
        HikariConfig config = new HikariConfig();
        config.setPoolName("shop-" + tenant);
        config.setJdbcUrl(entry.jdbcUrl());
        config.setUsername(entry.username());
        config.setPassword(entry.password());
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(3000);
        System.out.println("Opening pool shop-" + tenant + " for " + entry.jdbcUrl());
        return new HikariDataSource(config);
    }

    @Override
    public void stop() {
        shopPools.values().forEach(HikariDataSource::close);
        shopPools.clear();
    }

    @Override
    public boolean isUnwrappableAs(Class<?> type) {
        return type.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new UnknownUnwrapTypeException(type);
    }

}
