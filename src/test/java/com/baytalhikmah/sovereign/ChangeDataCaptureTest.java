package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.service.OrderDesk;

import jakarta.persistence.EntityManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Import(CdcTestcontainersConfiguration.class)
@SpringBootTest
class ChangeDataCaptureTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    OrderDesk orderDesk;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PostgreSQLContainer cdcPostgres;

    @Autowired
    KafkaContainer cdcKafka;

    @Autowired
    GenericContainer<?> debeziumConnect;

    @Test
    void theDatabaseLogCarriesTheOutboxEventToKafka() throws Exception {
        int httpStatus = registerConnector();
        waitForReplicationSlot("sovereign_outbox");

        transaction.executeWithoutResult(status -> {
            entityManager.persist(new Customer("Omar Haddad", "omar@cdc.test"));
            entityManager.persist(new Product("Q78-KETTLE", "Log Reader Kettle", Money.of("24.50", "EUR")));
        });
        orderDesk.placeOrder("Q78-0001", "omar@cdc.test", "Q78-KETTLE", 3);

        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cdcKafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "lab78-shipping",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.METADATA_MAX_AGE_CONFIG, "1000"),
                new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of("outbox.event.order"));
            Instant deadline = Instant.now().plusSeconds(60);
            while (received.isEmpty() && Instant.now().isBefore(deadline)) {
                consumer.poll(Duration.ofMillis(500)).forEach(received::add);
            }
        }
        ConsumerRecord<String, String> message = received.getFirst();
        Boolean published = jdbcTemplate.queryForObject(
            "select published_at is not null from sov_outbox_event where aggregate_id = 'Q78-0001'", Boolean.class);

        System.out.println("=== CHANGE DATA CAPTURE ===");
        System.out.println("connector        : HTTP " + httpStatus);
        System.out.println("topic            : " + message.topic());
        System.out.println("key              : " + message.key());
        System.out.println("header eventType : " + header(message, "eventType"));
        System.out.println("header id        : " + header(message, "id"));
        System.out.println("value            : " + message.value());
        System.out.println("published_at set : " + published);

        assertThat(httpStatus).isEqualTo(201);
        assertThat(received).hasSize(1);
        assertThat(message.topic()).isEqualTo("outbox.event.order");
        assertThat(message.key()).isEqualTo("Q78-0001");
        assertThat(header(message, "eventType")).isEqualTo("OrderPlaced");
        assertThat(message.value()).contains("\"orderNumber\": \"Q78-0001\"", "\"total\": 73.50");
        assertThat(published).isFalse();
    }

    /** Sends scripts/outbox-connector.json to Kafka Connect, with this test database's user and password. */
    private int registerConnector() throws Exception {
        Map<String, String> config = JSON.readValue(Files.readString(Path.of("scripts/outbox-connector.json")),
            new TypeReference<LinkedHashMap<String, String>>() {
            });
        config.put("database.user", cdcPostgres.getUsername());
        config.put("database.password", cdcPostgres.getPassword());

        URI uri = URI.create("http://" + debeziumConnect.getHost() + ":"
            + debeziumConnect.getMappedPort(CdcTestcontainersConfiguration.CONNECT_PORT)
            + "/connectors/sovereign-outbox/config");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(config)))
            .build();
        try (HttpClient http = HttpClient.newHttpClient()) {
            return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        }
    }

    /** Debezium is listening once its replication slot exists; every later commit reaches it. */
    private void waitForReplicationSlot(String slotName) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(60);
        while (Instant.now().isBefore(deadline)) {
            Integer slots = jdbcTemplate.queryForObject(
                "select count(*) from pg_replication_slots where slot_name = ?", Integer.class, slotName);
            if (slots != null && slots == 1) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Debezium did not create the replication slot " + slotName);
    }

    private static String header(ConsumerRecord<String, String> message, String name) {
        return new String(message.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

}
