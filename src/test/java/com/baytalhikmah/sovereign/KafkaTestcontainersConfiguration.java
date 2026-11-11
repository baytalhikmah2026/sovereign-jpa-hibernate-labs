package com.baytalhikmah.sovereign;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** A throw-away Kafka broker (KRaft mode) for the cache-invalidation tests. */
@TestConfiguration(proxyBeanMethods = false)
class KafkaTestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.2"));
	}

}
