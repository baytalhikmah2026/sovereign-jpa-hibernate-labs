package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.OrderDesk;
import com.baytalhikmah.sovereign.service.OutboxRelay;

import jakarta.persistence.EntityManager;

@Import({ TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class })
@ActiveProfiles("outbox-relay")
@SpringBootTest
class OutboxPatternTest {

    @Autowired
    OrderDesk orderDesk;

    @Autowired
    OutboxRelay outboxRelay;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    KafkaContainer kafkaContainer;

    @Test
    void theOrderAndItsMessageAreSavedTogether() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new Customer("Layla Nasser", "layla@outbox.test"));
            entityManager.persist(new Product("Q77-KETTLE", "Outbox Kettle", Money.of("39.00", "EUR")));
        });

        orderDesk.placeOrder("Q77-0001", "layla@outbox.test", "Q77-KETTLE", 2);
        Throwable sameOrderAgain = catchThrowable(
            () -> orderDesk.placeOrder("Q77-0001", "layla@outbox.test", "Q77-KETTLE", 2));
        Integer orders = jdbcTemplate.queryForObject(
            "select count(*) from sov_purchase_order where order_number = 'Q77-0001'", Integer.class);
        Integer events = jdbcTemplate.queryForObject(
            "select count(*) from sov_outbox_event where aggregate_id = 'Q77-0001'", Integer.class);

        String topic = OutboxRelay.topicFor(OrderDesk.ORDER_AGGREGATE);
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "lab77-shipping",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            outboxRelay.publishPending();
            Instant deadline = Instant.now().plusSeconds(15);
            while (received.isEmpty() && Instant.now().isBefore(deadline)) {
                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
            }
        }
        ConsumerRecord<String, String> message = received.getFirst();
        Boolean published = jdbcTemplate.queryForObject(
            "select published_at is not null from sov_outbox_event where aggregate_id = 'Q77-0001'", Boolean.class);

        System.out.println("=== OUTBOX ===");
        System.out.println("same order again : SQLSTATE " + SqlStates.of(sameOrderAgain));
        System.out.println("orders / events  : " + orders + " / " + events);
        System.out.println("topic            : " + message.topic());
        System.out.println("key              : " + message.key());
        System.out.println("header id        : "
            + new String(message.headers().lastHeader("id").value(), StandardCharsets.UTF_8));
        System.out.println("value            : " + message.value());
        System.out.println("marked published : " + published);

        assertThat(SqlStates.of(sameOrderAgain)).isEqualTo("23505");
        assertThat(orders).isEqualTo(1);
        assertThat(events).isEqualTo(1);
        assertThat(received).hasSize(1);
        assertThat(message.key()).isEqualTo("Q77-0001");
        assertThat(message.value()).contains("\"orderNumber\": \"Q77-0001\"", "\"total\": 78.00");
        assertThat(published).isTrue();
    }

}
