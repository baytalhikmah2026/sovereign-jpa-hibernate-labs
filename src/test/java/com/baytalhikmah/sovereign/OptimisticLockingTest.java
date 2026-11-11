package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.service.RetryingShipper;
import com.baytalhikmah.sovereign.service.RetryingShipper.Shipment;
import com.baytalhikmah.sovereign.service.StockDesk;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OptimisticLockingTest {

    static final InventoryItemId KETTLES = new InventoryItemId("Q58-WH", "KETTLE");

    @Autowired
    StockDesk stockDesk;

    @Autowired
    RetryingShipper retryingShipper;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void theVersionColumnStopsLostUpdates() {
        transaction.executeWithoutResult(status -> entityManager.persist(new InventoryItem("Q58-WH", "KETTLE", 10)));

        TransactionTemplate colleagueTransaction = new TransactionTemplate(transactionManager);
        colleagueTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Runnable colleagueShipsTwo = () -> colleagueTransaction.executeWithoutResult(status ->
            entityManager.find(InventoryItem.class, KETTLES).ship(2));

        System.out.println("=== OPTIMISTIC LOCKING ===");
        assertThatThrownBy(() -> stockDesk.shipOnce("Q58-WH", "KETTLE", 3, colleagueShipsTwo))
            .isInstanceOf(OptimisticLockingFailureException.class);
        InventoryItem afterConflict = reload();
        System.out.println("Without retry : conflict refused, stock " + afterConflict.getQuantityOnHand()
            + ", version " + afterConflict.getVersion());

        Shipment shipment = retryingShipper.shipWithRetry("Q58-WH", "KETTLE", 3, 3, colleagueShipsTwo);
        System.out.println("With retry    : shipped after " + shipment.attempts() + " attempts, stock "
            + shipment.stock().quantityOnHand() + ", version " + shipment.stock().version());

        assertThat(afterConflict.getQuantityOnHand()).isEqualTo(8);
        assertThat(afterConflict.getVersion()).isEqualTo(1);
        assertThat(shipment.attempts()).isEqualTo(2);
        assertThat(shipment.stock().quantityOnHand()).isEqualTo(3);
        assertThat(shipment.stock().version()).isEqualTo(3);
    }

    private InventoryItem reload() {
        return transaction.execute(status -> entityManager.find(InventoryItem.class, KETTLES));
    }

}
