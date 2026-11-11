package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.RetryingShipper;
import com.baytalhikmah.sovereign.service.RetryingShipper.Shipment;
import com.baytalhikmah.sovereign.service.StockDesk;
import com.baytalhikmah.sovereign.service.StockDesk.StockLevel;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LockTimeoutTest {

    static final String WAREHOUSE = "Q60-WH";

    @Autowired
    StockDesk stockDesk;

    @Autowired
    RetryingShipper retryingShipper;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    final ExecutorService colleague = Executors.newSingleThreadExecutor();

    /** A colleague's transaction that holds the write lock on KETTLE until released or until maxHold passes. */
    record Holder(CountDownLatch release, Future<StockLevel> done) {
        void finish() throws Exception {
            release.countDown();
            done.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void lockWaitsHaveALimit() throws Exception {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new InventoryItem(WAREHOUSE, "KETTLE", 10));
            entityManager.persist(new InventoryItem(WAREHOUSE, "MUG", 10));
            entityManager.persist(new InventoryItem(WAREHOUSE, "TEAPOT", 10));
        });
        System.out.println("=== LOCK TIMEOUTS ===");

        Holder holder = holdKettles(Duration.ofSeconds(10));
        long start = System.nanoTime();
        Throwable globalTimeout = catchThrowable(() -> stockDesk.shipLocked(WAREHOUSE, "KETTLE", 1, () -> { }));
        long globalMillis = millisSince(start);
        holder.finish();
        System.out.println("Global timeout : gave up after " + globalMillis + " ms, SQLSTATE " + SqlStates.of(globalTimeout));

        holder = holdKettles(Duration.ofSeconds(10));
        start = System.nanoTime();
        Throwable noWait = catchThrowable(() -> stockDesk.shipWithin(WAREHOUSE, "KETTLE", 1, Duration.ZERO));
        long noWaitMillis = millisSince(start);
        List<String> claimed = stockDesk.claimUnlockedSkus(WAREHOUSE, 10);
        holder.finish();
        System.out.println("No wait        : gave up after " + noWaitMillis + " ms, SQLSTATE " + SqlStates.of(noWait));
        System.out.println("Skip locked    : claimed " + claimed);

        holdKettles(Duration.ofSeconds(3));
        Shipment shipment = retryingShipper.shipWhenFree(WAREHOUSE, "KETTLE", 1, 3, Duration.ofMillis(500));
        System.out.println("Retry          : shipped after " + shipment.attempts() + " attempts, "
            + shipment.stock().quantityOnHand() + " kettles left");
        colleague.shutdown();
        colleague.awaitTermination(10, TimeUnit.SECONDS);
        int kettlesLeft = transaction.execute(status ->
            entityManager.find(InventoryItem.class, new InventoryItemId(WAREHOUSE, "KETTLE")).getQuantityOnHand());

        assertThat(globalTimeout).isInstanceOf(PessimisticLockingFailureException.class);
        assertThat(SqlStates.of(globalTimeout)).isEqualTo("55P03");
        assertThat(globalMillis).isBetween(1_500L, 5_000L);
        assertThat(noWait).isInstanceOf(PessimisticLockingFailureException.class);
        assertThat(SqlStates.of(noWait)).isEqualTo("55P03");
        assertThat(noWaitMillis).isLessThan(1_000L);
        assertThat(claimed).containsExactly("MUG", "TEAPOT");
        assertThat(shipment.attempts()).isEqualTo(2);
        assertThat(kettlesLeft).isEqualTo(10 - 3 - 1);
    }

    /** Starts the colleague: they lock KETTLE for writing, ship one, and keep the lock until released. */
    private Holder holdKettles(Duration maxHold) throws InterruptedException {
        CountDownLatch hasLock = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<StockLevel> done = colleague.submit(() -> stockDesk.shipLocked(WAREHOUSE, "KETTLE", 1, () -> {
            hasLock.countDown();
            try {
                release.await(maxHold.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }));
        hasLock.await(10, TimeUnit.SECONDS);
        return new Holder(release, done);
    }

    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

}
