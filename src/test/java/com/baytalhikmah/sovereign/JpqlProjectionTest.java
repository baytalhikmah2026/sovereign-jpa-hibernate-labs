package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.OrderStatus;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.query.OrderQueries;
import com.baytalhikmah.sovereign.query.OrderSummary;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JpqlProjectionTest {

    static final String DOMAIN = QueryLabData.emailDomain("q26");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderQueries orderQueries;

    @Test
    void aProjectionLoadsOnlyWhatTheScreenNeeds() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q26"));

        transaction.executeWithoutResult(status -> {
            Session session = entityManager.unwrap(Session.class);

            entityManager.createQuery("""
                    select o from PurchaseOrder o
                    join fetch o.customer c
                    join fetch o.lines
                    where c.email like :domain""", PurchaseOrder.class)
                .setParameter("domain", "%" + DOMAIN)
                .getResultList();
            int trackedAsEntities = session.getStatistics().getEntityCount();
            session.clear();

            List<OrderSummary> rows = orderQueries.summariesFor(DOMAIN);
            int trackedAfterProjection = session.getStatistics().getEntityCount();

            System.out.println("=== ORDER SUMMARIES (projection) ===");
            rows.forEach(row -> System.out.println(String.format("%-6s | %-11s | %-9s | %d lines | %s",
                row.orderNumber(), row.customerName(), row.status(), row.lineCount(), row.total())));
            System.out.println("Entities tracked after loading whole orders : " + trackedAsEntities);
            System.out.println("Entities tracked after the projection      : " + trackedAfterProjection);

            assertThat(rows).hasSize(6);
            OrderSummary first = rows.getFirst();
            assertThat(first.orderNumber()).isEqualTo("Q26-A1");
            assertThat(first.customerName()).isEqualTo("Amina Yusuf");
            assertThat(first.status()).isEqualTo(OrderStatus.PAID);
            assertThat(first.lineCount()).isEqualTo(2L);
            assertThat(first.total()).isEqualByComparingTo(new BigDecimal("63.50"));
            assertThat(trackedAsEntities).isEqualTo(18);
            assertThat(trackedAfterProjection).isZero();
        });
    }

}
