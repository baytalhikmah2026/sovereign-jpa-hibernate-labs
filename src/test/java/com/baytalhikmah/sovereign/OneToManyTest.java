package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.OrderLine;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OneToManyTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void helperMethodsKeepBothSidesInStep() {
        UUID orderId = placeOrder("SO-1001");

        transaction.executeWithoutResult(status -> {
            PurchaseOrder order = entityManager.find(PurchaseOrder.class, orderId);
            OrderLine first = order.getLines().get(0);

            System.out.println("=== BOTH SIDES ===");
            System.out.println("Lines on the order : " + order.getLines().size());
            System.out.println("Line knows order   : " + first.getOrder().getOrderNumber());
            System.out.println("Order total        : " + order.total());

            assertThat(first.getOrder()).isSameAs(order);
            assertThat(order.total()).isEqualTo(Money.of("91.00", "GBP"));
        });
        assertThat(lineCount(orderId)).isEqualTo(2);
    }

    @Test
    void orphanRemovalDeletesARemovedLine() {
        UUID orderId = placeOrder("SO-1002");

        transaction.executeWithoutResult(status -> {
            PurchaseOrder order = entityManager.find(PurchaseOrder.class, orderId);
            order.removeLine(order.getLines().get(0));
        });

        System.out.println("=== ORPHAN REMOVAL ===");
        System.out.println("Lines left in the database : " + lineCount(orderId));
        assertThat(lineCount(orderId)).isEqualTo(1);
    }

    @Test
    void removingTheOrderCascadesToItsLines() {
        UUID orderId = placeOrder("SO-1003");

        transaction.executeWithoutResult(status ->
            entityManager.remove(entityManager.find(PurchaseOrder.class, orderId)));

        System.out.println("=== CASCADE REMOVE ===");
        System.out.println("Lines left in the database : " + lineCount(orderId));
        assertThat(lineCount(orderId)).isZero();
    }

    private UUID placeOrder(String orderNumber) {
        return transaction.execute(status -> {
            Customer bilal = new Customer("Bilal Ahmed", orderNumber.toLowerCase() + "@orders.sovereign.test");
            Product kettle = new Product("KET-" + orderNumber, "Copper Kettle", Money.of("39.50", "GBP"));
            Product mug = new Product("MUG-" + orderNumber, "Stoneware Mug", Money.of("6.00", "GBP"));
            entityManager.persist(bilal);
            entityManager.persist(kettle);
            entityManager.persist(mug);

            PurchaseOrder order = new PurchaseOrder(orderNumber, bilal);
            order.addLine(kettle, 2);
            order.addLine(mug, 2);
            entityManager.persist(order);
            return order.getId();
        });
    }

    private int lineCount(UUID orderId) {
        return jdbc.queryForObject("select count(*) from sov_order_line where order_id = ?", Integer.class, orderId);
    }

}
