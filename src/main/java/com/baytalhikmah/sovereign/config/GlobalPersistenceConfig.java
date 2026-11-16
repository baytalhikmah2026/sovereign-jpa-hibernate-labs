package com.baytalhikmah.sovereign.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.baytalhikmah.sovereign.persistence.GlobalPersistenceUnit;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;
import com.baytalhikmah.sovereign.service.GlobalStockDesk;

/** With the profile cockroach, the application also opens the global persistence unit on the CockroachDB cluster. */
@Configuration(proxyBeanMethods = false)
@Profile("cockroach")
public class GlobalPersistenceConfig {

    @Bean(destroyMethod = "close")
    GlobalPersistenceUnit globalPersistenceUnit(@Value("${sovereign.global.url}") String url,
            @Value("${sovereign.global.username}") String username,
            @Value("${sovereign.global.password:}") String password) {
        return new GlobalPersistenceUnit(url, username, password);
    }

    @Bean
    GlobalStockDesk globalStockDesk(GlobalPersistenceUnit global) {
        return new GlobalStockDesk(new TransactionRetry(global.entityManagerFactory(), 5, Duration.ofMillis(50)));
    }

}
