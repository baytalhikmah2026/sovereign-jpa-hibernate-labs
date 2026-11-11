package com.baytalhikmah.sovereign.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import com.baytalhikmah.sovereign.persistence.CacheInvalidationListener;

/** Creates the price-change topic when the invalidation profile is active. */
@Configuration(proxyBeanMethods = false)
@Profile("invalidation")
public class CacheInvalidationConfig {

    @Bean
    NewTopic priceChangesTopic() {
        return TopicBuilder.name(CacheInvalidationListener.PRICE_CHANGES).partitions(1).replicas(1).build();
    }

}
