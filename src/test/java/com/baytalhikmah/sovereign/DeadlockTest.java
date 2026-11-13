package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.PairedShipment;
import com.baytalhikmah.sovereign.service.StockDesk;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DeadlockTest {

    static final String WAREHOUSE = "Q69-WH";

    @Autowired
    PairedShipment pairedShipment;

    @Autowired
    StockDesk stockDesk;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    final List<String> databaseMessages = new ArrayList<>();

    @Test
    void oppositeLockOrderDeadlocksAndFixedOrderDoesNot() throws Exception {
        stockDesk.register(WAREHOUSE, "KETTLE", 10);
        stockDesk.register(WAREHOUSE, "MUG", 10);

        CyclicBarrier bothHoldOneLock = new CyclicBarrier(2);
        Runnable meet = () -> await(bothHoldOneLock);
        List<String> crossedOrder = runTogether(
            () -> pairedShipment.shipPair(WAREHOUSE, "KETTLE", "MUG", 1, meet),
            () -> pairedShipment.shipPair(WAREHOUSE, "MUG", "KETTLE", 1, meet));

        Runnable overlap = () -> pause(300);
        List<String> fixedOrder = runTogether(
            () -> pairedShipment.shipPairInFixedOrder(WAREHOUSE, "KETTLE", "MUG", 1, overlap),
            () -> pairedShipment.shipPairInFixedOrder(WAREHOUSE, "MUG", "KETTLE", 1, overlap));

        System.out.println("=== DEADLOCK AUTOPSY ===");
        System.out.println("Crossed lock order : " + crossedOrder);
        System.out.println("Database report    : " + databaseMessages);
        System.out.println("Fixed lock order   : " + fixedOrder);
        System.out.println("Stock left         : KETTLE=" + quantity("KETTLE") + ", MUG=" + quantity("MUG"));

        assertThat(crossedOrder).containsExactly("failed 40P01", "shipped");
        assertThat(databaseMessages).singleElement().asString()
            .contains("deadlock detected")
            .contains("waits for ShareLock on transaction");
        assertThat(fixedOrder).containsExactly("shipped", "shipped");
        assertThat(quantity("KETTLE")).isEqualTo(7);
        assertThat(quantity("MUG")).isEqualTo(7);
    }

    /** Runs both jobs at the same time on two named threads; returns "shipped" or "failed <SQLSTATE>" per job. */
    private List<String> runTogether(Supplier<?> first, Supplier<?> second) throws Exception {
        ExecutorService buyers = Executors.newFixedThreadPool(2, Thread.ofPlatform().name("buyer-", 1).factory());
        try {
            List<Future<String>> outcomes = List.of(buyers.submit(() -> attempt(first)),
                buyers.submit(() -> attempt(second)));
            List<String> results = new ArrayList<>();
            for (Future<String> outcome : outcomes) {
                results.add(outcome.get(10, TimeUnit.SECONDS));
            }
            return results.stream().sorted().toList();
        }
        finally {
            buyers.shutdownNow();
        }
    }

    private String attempt(Supplier<?> job) {
        try {
            job.get();
            return "shipped";
        }
        catch (RuntimeException failure) {
            synchronized (databaseMessages) {
                databaseMessages.add(SqlStates.messageOf(failure));
            }
            return "failed " + SqlStates.of(failure);
        }
    }

    private int quantity(String sku) {
        return transaction.execute(status -> entityManager
            .find(InventoryItem.class, new InventoryItemId(WAREHOUSE, sku))
            .getQuantityOnHand());
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        }
        catch (Exception interrupted) {
            throw new IllegalStateException(interrupted);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

}
