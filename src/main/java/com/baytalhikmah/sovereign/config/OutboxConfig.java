package com.baytalhikmah.sovereign.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.baytalhikmah.sovereign.service.OrderDesk;
import com.baytalhikmah.sovereign.service.OutboxRelay;

/** Switches on the outbox relay's timer and creates the order-events topic. */
@Configuration(proxyBeanMethods = false)
@Profile("outbox-relay")
@EnableScheduling
public class OutboxConfig {

    /** Three partitions; the order number is the message key, so all events of one order stay in order. */
    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OutboxRelay.topicFor(OrderDesk.ORDER_AGGREGATE)).partitions(3).replicas(1).build();
    }

}
