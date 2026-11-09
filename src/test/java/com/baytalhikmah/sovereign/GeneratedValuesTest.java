package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.OrderLine;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GeneratedValuesTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void theDatabaseFillsValuesAndHibernateReadsThemBack() {
        Instant before = Instant.now().minusSeconds(5);

        transaction.executeWithoutResult(status -> {
            Customer ibrahim = new Customer("Ibrahim Farooq", "ibrahim.generated@sovereign.test");
            Product kettle = new Product("KET-GEN", "Copper Kettle", Money.of("39.50", "GBP"));
            entityManager.persist(ibrahim);
            entityManager.persist(kettle);

            PurchaseOrder order = new PurchaseOrder("SO-4001", ibrahim);
            OrderLine line = order.addLine(kettle, 2);
            System.out.println("=== BEFORE FLUSH ===");
            System.out.println("placedAt        : " + order.getPlacedAt());
            System.out.println("lineTotalAmount : " + line.getLineTotalAmount());

            entityManager.persist(order);
            entityManager.flush();

            System.out.println("=== AFTER FLUSH (same transaction, no reload) ===");
            System.out.println("placedAt        : " + order.getPlacedAt());
            System.out.println("lineTotalAmount : " + line.getLineTotalAmount());

            assertThat(order.getPlacedAt()).isAfter(before);
            assertThat(line.getLineTotalAmount()).isEqualByComparingTo(new BigDecimal("79.00"));
        });
    }

}
