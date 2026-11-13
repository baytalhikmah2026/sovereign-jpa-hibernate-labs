package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.service.StockDesk;
import com.baytalhikmah.sovereign.service.WarehouseGateway;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EntityListenerTest {

    static final String WAREHOUSE = "Q67-WH";

    @Autowired
    StockDesk stockDesk;

    @Autowired
    WarehouseGateway gateway;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void externalSystemHearsOnlyCommittedChanges() {
        stockDesk.register(WAREHOUSE, "KETTLE", 10);
        stockDesk.shipOnce(WAREHOUSE, "KETTLE", 3, () -> { });
        List<String> afterCommits = gateway.sentFor(WAREHOUSE);

        transaction.executeWithoutResult(status -> {
            stockDesk.shipOnce(WAREHOUSE, "KETTLE", 5, () -> { });
            System.out.println("Shipped 5 inside a transaction that will be rolled back");
            status.setRollbackOnly();
        });
        List<String> afterRollback = gateway.sentFor(WAREHOUSE);
        int storedQuantity = transaction.execute(status -> entityManager
            .find(InventoryItem.class, new InventoryItemId(WAREHOUSE, "KETTLE"))
            .getQuantityOnHand());

        System.out.println("=== ENTITY LISTENER AND AFTER-COMMIT SYNC ===");
        System.out.println("Sent after two commits : " + afterCommits);
        System.out.println("Sent after a rollback  : " + afterRollback);
        System.out.println("Stored quantity        : " + storedQuantity);

        assertThat(afterCommits).containsExactly(
            "Q67-WH/KETTLE on hand 10 (version 0)",
            "Q67-WH/KETTLE on hand 7 (version 1)");
        assertThat(afterRollback).isEqualTo(afterCommits);
        assertThat(storedQuantity).isEqualTo(7);
    }

}
