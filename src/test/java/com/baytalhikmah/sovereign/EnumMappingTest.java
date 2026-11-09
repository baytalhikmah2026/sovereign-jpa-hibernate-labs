package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.OrderStatus;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EnumMappingTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void statusesAreStoredAsStableCodes() {
        UUID orderId = transaction.execute(status -> {
            Customer omar = new Customer("Omar Khan", "omar.status@sovereign.test");
            entityManager.persist(omar);
            PurchaseOrder order = new PurchaseOrder("SO-3001", omar);
            entityManager.persist(order);
            return order.getId();
        });
        String afterCreate = storedCode(orderId);

        transaction.executeWithoutResult(status -> {
            PurchaseOrder order = entityManager.find(PurchaseOrder.class, orderId);
            order.moveTo(OrderStatus.PAID);
            order.moveTo(OrderStatus.SHIPPED);
        });
        PurchaseOrder reloaded = transaction.execute(status -> entityManager.find(PurchaseOrder.class, orderId));

        System.out.println("=== ENUM CODES ===");
        System.out.println("Stored after create : " + afterCreate);
        System.out.println("Stored after ship   : " + storedCode(orderId));
        System.out.println("Read back in Java   : " + reloaded.getStatus());

        assertThat(afterCreate).isEqualTo("NW");
        assertThat(storedCode(orderId)).isEqualTo("SH");
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThatThrownBy(() -> reloaded.moveTo(OrderStatus.CANCELLED))
            .hasMessageContaining("cannot move from SHIPPED to CANCELLED");
    }

    @Test
    void theDatabaseRefusesUnknownCodes() {
        UUID orderId = transaction.execute(status -> {
            Customer sara = new Customer("Sara Ali", "sara.status@sovereign.test");
            entityManager.persist(sara);
            PurchaseOrder order = new PurchaseOrder("SO-3002", sara);
            entityManager.persist(order);
            return order.getId();
        });

        System.out.println("=== CHECK CONSTRAINT ===");
        assertThatThrownBy(() -> jdbc.update("update sov_purchase_order set status = 'XX' where id = ?", orderId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("sov_purchase_order_status_check");
        System.out.println("Writing 'XX' directly : refused");
        System.out.println("Check constraint      : " + checkConstraint());
    }

    private String checkConstraint() {
        return jdbc.queryForObject("""
            select conname || ' ' || pg_get_constraintdef(oid) from pg_constraint
            where conrelid = 'sov_purchase_order'::regclass and contype = 'c'""", String.class);
    }

    private String storedCode(UUID orderId) {
        return jdbc.queryForObject("select status from sov_purchase_order where id = ?", String.class, orderId);
    }

}
