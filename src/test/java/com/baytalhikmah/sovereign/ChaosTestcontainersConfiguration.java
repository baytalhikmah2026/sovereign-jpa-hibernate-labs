package com.baytalhikmah.sovereign;

import java.io.IOException;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;

/** PostgreSQL behind Toxiproxy: the application reaches the database only through the fault-injecting proxy. */
@TestConfiguration(proxyBeanMethods = false)
class ChaosTestcontainersConfiguration {

	static final int PROXY_PORT = 8666;

	@Bean
	Network chaosNetwork() {
		return Network.newNetwork();
	}

	@Bean
	PostgreSQLContainer chaosPostgres(Network chaosNetwork) {
		return new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE)
			.withNetwork(chaosNetwork)
			.withNetworkAliases("postgres")
			.withDatabaseName("sovereign")
			.withUsername("sovereign_app")
			.withPassword(UUID.randomUUID().toString())
			.withInitScript("db/test-extensions.sql");
	}

	@Bean
	ToxiproxyContainer toxiproxy(Network chaosNetwork) {
		return new ToxiproxyContainer(DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.13.0"))
			.withNetwork(chaosNetwork);
	}

	@Bean
	Proxy postgresProxy(ToxiproxyContainer toxiproxy, PostgreSQLContainer chaosPostgres) throws IOException {
		ToxiproxyClient client = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
		return client.createProxy("postgres", "0.0.0.0:" + PROXY_PORT, "postgres:5432");
	}

	@Bean
	DynamicPropertyRegistrar chaosDataSource(ToxiproxyContainer toxiproxy, PostgreSQLContainer chaosPostgres,
			Proxy postgresProxy) {
		return registry -> {
			registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + toxiproxy.getHost() + ":"
				+ toxiproxy.getMappedPort(PROXY_PORT) + "/sovereign");
			registry.add("spring.datasource.username", chaosPostgres::getUsername);
			registry.add("spring.datasource.password", chaosPostgres::getPassword);
		};
	}

}
