package com.baytalhikmah.sovereign;

import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	static final DockerImageName POSTGRES_IMAGE = DockerImageName
		.parse("pgvector/pgvector:0.8.7-pg18")
		.asCompatibleSubstituteFor("postgres");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(POSTGRES_IMAGE)
			.withDatabaseName("sovereign")
			.withUsername("sovereign_app")
			.withPassword(UUID.randomUUID().toString());
	}

}
