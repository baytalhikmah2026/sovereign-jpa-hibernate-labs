package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.query.OrderReceipt;
import com.baytalhikmah.sovereign.query.ReceiptQueries;
import com.baytalhikmah.sovereign.service.OrderOverview;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DtoOverEntitiesTest {

    static final String KEY = "q51";

    record Comparison(int orders, int managedByEntities, long entityStatements,
            List<OrderReceipt> receipts, int managedByDtos, long dtoStatements) {
    }

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Autowired
    ReceiptQueries receiptQueries;

    @Test
    void aReadOnlyScreenNeedsNoManagedEntities() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, KEY));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        String domain = QueryLabData.emailDomain(KEY);

        Comparison comparison = transaction.execute(status -> {
            Session session = entityManager.unwrap(Session.class);

            statistics.clear();
            List<PurchaseOrder> orders = orderOverview.ordersWithCustomerAndLines(domain);
            int managedByEntities = session.getStatistics().getEntityCount();
            long entityStatements = statistics.getPrepareStatementCount();

            entityManager.clear();
            statistics.clear();
            List<OrderReceipt> receipts = receiptQueries.receiptsFor(domain);
            return new Comparison(orders.size(), managedByEntities, entityStatements,
                receipts, session.getStatistics().getEntityCount(), statistics.getPrepareStatementCount());
        });

        System.out.println("=== ENTITIES VS DTOS ===");
        System.out.println("Entities : " + comparison.orders() + " orders, " + comparison.managedByEntities()
            + " objects watched by the persistence context, " + comparison.entityStatements() + " statement");
        System.out.println("DTOs     : " + comparison.receipts().size() + " receipts, " + comparison.managedByDtos()
            + " objects watched by the persistence context, " + comparison.dtoStatements() + " statement");
        System.out.println("--- receipts (read after the transaction) ---");
        for (OrderReceipt receipt : comparison.receipts()) {
            System.out.println(receipt.orderNumber() + " " + receipt.customerName() + " : "
                + receipt.lines().size() + " lines, total " + receipt.total());
        }

        assertThat(comparison.orders()).isEqualTo(6);
        assertThat(comparison.managedByEntities()).isEqualTo(18);
        assertThat(comparison.entityStatements()).isEqualTo(1);
        assertThat(comparison.receipts()).extracting(OrderReceipt::orderNumber)
            .containsExactly("Q51-A1", "Q51-A2", "Q51-A3", "Q51-B1", "Q51-B2", "Q51-Z1");
        assertThat(comparison.receipts().get(0).lines()).extracting(line -> line.productName())
            .containsExactly("Copper Kettle", "Stoneware Mug");
        assertThat(comparison.receipts().get(0).total()).isEqualByComparingTo("63.50");
        assertThat(comparison.managedByDtos()).isZero();
        assertThat(comparison.dtoStatements()).isEqualTo(1);
    }

}
