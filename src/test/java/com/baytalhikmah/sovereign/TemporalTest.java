package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TemporalTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void oneMomentReadsTheSameEverywhere() {
        ZonedDateTime appointment = ZonedDateTime.of(2026, 11, 20, 14, 0, 0, 0, ZoneId.of("Asia/Karachi"));

        UUID orderId = transaction.execute(status -> {
            Customer hamza = new Customer("Hamza Siddiqui", "hamza.time@sovereign.test");
            entityManager.persist(hamza);
            PurchaseOrder order = new PurchaseOrder("SO-5001", hamza);
            order.promiseDelivery(appointment);
            entityManager.persist(order);
            return order.getId();
        });

        PurchaseOrder loaded = transaction.execute(status -> entityManager.find(PurchaseOrder.class, orderId));
        Map<String, Object> stored = jdbc.queryForMap(
            "select delivery_promised_for::text as utc_text, delivery_promised_for_tz as offset_column from sov_purchase_order where id = ?",
            orderId);
        String inLondon = textInSessionZone("Europe/London", orderId);
        String inNewYork = textInSessionZone("America/New_York", orderId);

        System.out.println("=== TIME ===");
        System.out.println("Promised (Java, in)  : " + appointment);
        System.out.println("Promised (Java, out) : " + loaded.getDeliveryPromisedFor());
        System.out.println("Stored timestamp     : " + stored.get("utc_text"));
        System.out.println("Stored offset column : " + stored.get("offset_column"));
        System.out.println("Seen from London     : " + inLondon);
        System.out.println("Seen from New York   : " + inNewYork);
        System.out.println("Placed at (Instant)  : " + loaded.getPlacedAt());

        assertThat(loaded.getDeliveryPromisedFor().toInstant()).isEqualTo(appointment.toInstant());
        assertThat(loaded.getDeliveryPromisedFor().getOffset()).isEqualTo(appointment.getOffset());
        assertThat(inLondon).isNotEqualTo(inNewYork);
    }

    private String textInSessionZone(String zone, UUID orderId) {
        return transaction.execute(status -> {
            jdbc.execute("set local time zone '" + zone + "'");
            return jdbc.queryForObject(
                "select delivery_promised_for::text from sov_purchase_order where id = ?", String.class, orderId);
        });
    }

}
