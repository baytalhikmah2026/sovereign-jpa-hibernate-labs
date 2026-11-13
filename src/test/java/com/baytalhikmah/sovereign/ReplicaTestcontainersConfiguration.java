package com.baytalhikmah.sovereign;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** A primary PostgreSQL and a streaming replica of it, on one private Docker network. */
@TestConfiguration(proxyBeanMethods = false)
class ReplicaTestcontainersConfiguration {

	@Bean
	Network replicationNetwork() {
		return Network.newNetwork();
	}

	@Bean
	PostgreSQLContainer primaryPostgres(Network replicationNetwork) {
		return new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE)
			.withNetwork(replicationNetwork)
			.withNetworkAliases("primary")
			.withDatabaseName("sovereign")
			.withUsername("sovereign_app")
			.withPassword(UUID.randomUUID().toString())
			.withCopyFileToContainer(MountableFile.forHostPath("scripts/enable-replication.sh", 0755),
				"/docker-entrypoint-initdb.d/enable-replication.sh")
			.withInitScript("db/test-extensions.sql");
	}

	@Bean
	GenericContainer<?> replicaPostgres(Network replicationNetwork, PostgreSQLContainer primaryPostgres) {
		return new GenericContainer<>(TestcontainersConfiguration.POSTGRES_IMAGE)
			.withNetwork(replicationNetwork)
			.withNetworkAliases("replica")
			.withCreateContainerCmdModifier(command -> command.withUser("postgres"))
			.withEnv("PRIMARY_HOST", "primary")
			.withEnv("PGPASSWORD", "replica-local-only")
			.withCopyFileToContainer(MountableFile.forHostPath("scripts/replica-start.sh", 0755), "/replica-start.sh")
			.withCommand("sh", "/replica-start.sh")
			.withExposedPorts(5432)
			.waitingFor(Wait.forLogMessage(".*database system is ready to accept read-only connections.*\\s", 1));
	}

	@Bean
	DynamicPropertyRegistrar replicationDataSources(PostgreSQLContainer primaryPostgres,
			@Qualifier("replicaPostgres") GenericContainer<?> replicaPostgres) {
		return registry -> {
			registry.add("spring.datasource.url", primaryPostgres::getJdbcUrl);
			registry.add("spring.datasource.username", primaryPostgres::getUsername);
			registry.add("spring.datasource.password", primaryPostgres::getPassword);
			registry.add("sovereign.replica.url", () -> "jdbc:postgresql://" + replicaPostgres.getHost() + ":"
				+ replicaPostgres.getMappedPort(5432) + "/sovereign");
		};
	}

}
