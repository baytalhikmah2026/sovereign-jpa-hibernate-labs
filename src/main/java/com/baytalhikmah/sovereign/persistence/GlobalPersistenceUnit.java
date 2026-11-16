package com.baytalhikmah.sovereign.persistence;

import javax.sql.DataSource;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

import com.baytalhikmah.sovereign.domain.RegionalStock;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * A second, small persistence unit for the data the whole chain shares: it runs on the distributed SQL cluster
 * (CockroachDB) with Hibernate's CockroachDB dialect, next to the main unit on PostgreSQL. Built with the
 * Jakarta Persistence 3.2 bootstrap API, so it never replaces Spring Boot's own EntityManagerFactory.
 */
public class GlobalPersistenceUnit implements AutoCloseable {

    private final DataSource dataSource;

    private final EntityManagerFactory entityManagerFactory;

    public GlobalPersistenceUnit(String jdbcUrl, String username, String password) {
        this(pool("GlobalPool", jdbcUrl, username, password));
    }

    public GlobalPersistenceUnit(DataSource dataSource) {
        this.dataSource = dataSource;
        this.entityManagerFactory = new HibernatePersistenceConfiguration("global")
            .managedClass(RegionalStock.class)
            .property(JdbcSettings.DATASOURCE, dataSource)
            .property(JdbcSettings.DIALECT, CockroachDialect.class.getName())
            .property(MappingSettings.PHYSICAL_NAMING_STRATEGY, SovereignNamingStrategy.class.getName())
            .schemaToolingAction(Action.UPDATE)
            .createEntityManagerFactory();
    }

    /** A small HikariCP pool for one cluster endpoint. */
    public static HikariDataSource pool(String name, String jdbcUrl, String username, String password) {
        HikariConfig pool = new HikariConfig();
        pool.setPoolName(name);
        pool.setJdbcUrl(jdbcUrl);
        pool.setUsername(username);
        pool.setPassword(password);
        pool.setMaximumPoolSize(5);
        return new HikariDataSource(pool);
    }

    public EntityManagerFactory entityManagerFactory() {
        return entityManagerFactory;
    }

    @Override
    public void close() throws Exception {
        entityManagerFactory.close();
        if (dataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

}
