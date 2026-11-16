package com.baytalhikmah.sovereign.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.baytalhikmah.sovereign.persistence.GlobalPersistenceUnit;
import com.baytalhikmah.sovereign.persistence.NearestEndpointDataSource;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;
import com.baytalhikmah.sovereign.service.GlobalStockDesk;

/**
 * With the profile cockroach, the application also opens the global persistence unit on the CockroachDB cluster,
 * through the endpoint (gateway node) nearest to this application copy.
 */
@Configuration(proxyBeanMethods = false)
@Profile("cockroach")
@EnableConfigurationProperties(GlobalPersistenceConfig.GlobalCluster.class)
public class GlobalPersistenceConfig {

    /** sovereign.global.*: the database, the login and one host:port per region. */
    @ConfigurationProperties("sovereign.global")
    public record GlobalCluster(String database, String username, String password, Map<String, String> endpoints) {
    }

    /**
     * The router is created here and handed to the unit, never registered as a DataSource bean of its own:
     * Spring Boot would then skip the application's main PostgreSQL pool.
     */
    @Bean(destroyMethod = "close")
    GlobalPersistenceUnit globalPersistenceUnit(GlobalCluster cluster) {
        Map<String, DataSource> pools = new LinkedHashMap<>();
        cluster.endpoints().forEach((region, hostAndPort) -> pools.put(region, GlobalPersistenceUnit.pool(
            "Global-" + region, "jdbc:postgresql://" + hostAndPort + "/" + cluster.database() + "?sslmode=disable",
            cluster.username(), cluster.password() == null ? "" : cluster.password())));
        return new GlobalPersistenceUnit(new NearestEndpointDataSource(pools));
    }

    @Bean
    GlobalStockDesk globalStockDesk(GlobalPersistenceUnit global) {
        return new GlobalStockDesk(new TransactionRetry(global.entityManagerFactory(), 5, Duration.ofMillis(50)));
    }

}
