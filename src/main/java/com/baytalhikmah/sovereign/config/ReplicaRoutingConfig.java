package com.baytalhikmah.sovereign.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import com.zaxxer.hikari.HikariDataSource;

/**
 * With the replica profile the application has two pools: one to the primary (all writes) and one to the
 * read-only replica. Read-only transactions are sent to the replica, everything else to the primary.
 */
@Configuration(proxyBeanMethods = false)
@Profile("replica")
public class ReplicaRoutingConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource primaryPool(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("sovereign.replica.hikari")
    HikariDataSource replicaPool(DataSourceProperties properties, @Value("${sovereign.replica.url}") String replicaUrl) {
        HikariDataSource replica = new HikariDataSource();
        replica.setJdbcUrl(replicaUrl);
        replica.setUsername(properties.determineUsername());
        replica.setPassword(properties.determinePassword());
        return replica;
    }

    /**
     * The DataSource that JPA, JdbcTemplate and Spring's transactions use. It hands out a placeholder
     * connection and only picks a real pool at the first statement: the replica pool when the
     * transaction was marked read-only, the primary pool otherwise.
     */
    @Bean
    @Primary
    DataSource dataSource(@Qualifier("primaryPool") DataSource primaryPool,
            @Qualifier("replicaPool") DataSource replicaPool) {
        LazyConnectionDataSourceProxy routing = new LazyConnectionDataSourceProxy(primaryPool);
        routing.setReadOnlyDataSource(replicaPool);
        return routing;
    }

}
