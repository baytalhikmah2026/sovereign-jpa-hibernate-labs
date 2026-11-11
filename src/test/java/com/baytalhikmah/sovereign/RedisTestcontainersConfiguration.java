package com.baytalhikmah.sovereign;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** A throw-away Redis for the second-level cache tests, and its address for SovereignRedissonRegionFactory. */
@TestConfiguration(proxyBeanMethods = false)
class RedisTestcontainersConfiguration {

	@Bean
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:8.10.3")).withExposedPorts(6379);
	}

	@Bean
	DynamicPropertyRegistrar redisAddress(@Qualifier("redisContainer") GenericContainer<?> redisContainer) {
		return registry -> registry.add("spring.jpa.properties.sovereign.cache.redis-address",
			() -> "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379));
	}

}
