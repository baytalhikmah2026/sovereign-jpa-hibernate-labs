package com.baytalhikmah.sovereign;

import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL with logical decoding, a Kafka broker and a Debezium (Kafka Connect) worker on one private network,
 * reachable there as "postgres", "kafka" and "connect", like the Compose services.
 */
@TestConfiguration(proxyBeanMethods = false)
class CdcTestcontainersConfiguration {

    static final int CONNECT_PORT = 8083;

    @Bean
    Network cdcNetwork() {
        return Network.newNetwork();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer cdcPostgres(Network cdcNetwork) {
        return new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE)
            .withNetwork(cdcNetwork)
            .withNetworkAliases("postgres")
            .withDatabaseName("sovereign")
            .withUsername("sovereign_app")
            .withPassword(UUID.randomUUID().toString())
            .withInitScript("db/test-extensions.sql")
            .withCommand("postgres", "-c", "wal_level=logical");
    }

    @Bean
    @ServiceConnection
    KafkaContainer cdcKafka(Network cdcNetwork) {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.2"))
            .withNetwork(cdcNetwork)
            .withNetworkAliases("kafka")
            .withListener("kafka:19092");
    }

    @Bean
    GenericContainer<?> debeziumConnect(Network cdcNetwork, KafkaContainer cdcKafka, PostgreSQLContainer cdcPostgres) {
        return new GenericContainer<>(DockerImageName.parse("quay.io/debezium/connect:3.7.1.Final"))
            .withNetwork(cdcNetwork)
            .withNetworkAliases("connect")
            .withEnv("BOOTSTRAP_SERVERS", "kafka:19092")
            .withEnv("GROUP_ID", "sovereign-connect")
            .withEnv("CONFIG_STORAGE_TOPIC", "connect_configs")
            .withEnv("OFFSET_STORAGE_TOPIC", "connect_offsets")
            .withEnv("STATUS_STORAGE_TOPIC", "connect_statuses")
            .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withExposedPorts(CONNECT_PORT)
            .dependsOn(cdcKafka, cdcPostgres)
            .waitingFor(Wait.forHttp("/connectors").forPort(CONNECT_PORT).withStartupTimeout(Duration.ofMinutes(3)));
    }

}
