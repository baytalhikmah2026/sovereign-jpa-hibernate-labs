package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.service.StockDesk;
import com.baytalhikmah.sovereign.service.StockDesk.StockLevel;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PessimisticLockTest {

    static final String WAREHOUSE = "Q59-WH";
    static final String SKU = "KETTLE";

    @Autowired
    StockDesk stockDesk;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void readLocksAreSharedAndWriteLocksWait() throws Exception {
        transaction.executeWithoutResult(status -> entityManager.persist(new InventoryItem(WAREHOUSE, SKU, 10)));
        ExecutorService otherClerk = Executors.newSingleThreadExecutor();
        System.out.println("=== ROW LOCKS ===");

        CountDownLatch readerHasLock = new CountDownLatch(1);
        CountDownLatch readerMayFinish = new CountDownLatch(1);
        Future<StockLevel> firstReader = otherClerk.submit(() ->
            stockDesk.countLocked(WAREHOUSE, SKU, () -> holdLock(readerHasLock, readerMayFinish)));
        readerHasLock.await(10, TimeUnit.SECONDS);
        long start = System.nanoTime();
        StockLevel secondReader = stockDesk.countLocked(WAREHOUSE, SKU, () -> { });
        long secondReaderMillis = (System.nanoTime() - start) / 1_000_000;
        readerMayFinish.countDown();
        firstReader.get(10, TimeUnit.SECONDS);
        System.out.println("Second reader got its lock after " + secondReaderMillis + " ms, sees "
            + secondReader.quantityOnHand() + " kettles");

        CountDownLatch writerBlockerHasLock = new CountDownLatch(1);
        CountDownLatch blockerMayFinish = new CountDownLatch(1);
        Future<StockLevel> blockingReader = otherClerk.submit(() ->
            stockDesk.countLocked(WAREHOUSE, SKU, () -> holdLock(writerBlockerHasLock, blockerMayFinish)));
        writerBlockerHasLock.await(10, TimeUnit.SECONDS);
        ExecutorService writerThread = Executors.newSingleThreadExecutor();
        Future<StockLevel> writer = writerThread.submit(() -> stockDesk.shipLocked(WAREHOUSE, SKU, 3, () -> { }));
        List<String> waiting = waitingQueries();
        System.out.println("Waiting for a lock : " + waiting);
        blockerMayFinish.countDown();
        blockingReader.get(10, TimeUnit.SECONDS);
        StockLevel afterShipping = writer.get(10, TimeUnit.SECONDS);
        System.out.println("Writer shipped after the reader finished: " + afterShipping.quantityOnHand()
            + " kettles left, version " + afterShipping.version());
        otherClerk.shutdown();
        writerThread.shutdown();

        assertThat(secondReaderMillis).isLessThan(1_000);
        assertThat(secondReader.quantityOnHand()).isEqualTo(10);
        assertThat(waiting).hasSize(1);
        assertThat(waiting.getFirst()).contains("for no key update");
        assertThat(afterShipping.quantityOnHand()).isEqualTo(7);
        assertThat(afterShipping.version()).isEqualTo(1);
    }

    /** Asks PostgreSQL which statements of this database are waiting for a lock, for up to 10 seconds. */
    private List<String> waitingQueries() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            List<String> queries = jdbc.queryForList(
                "select query from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock'",
                String.class);
            if (!queries.isEmpty()) {
                return queries;
            }
            Thread.sleep(100);
        }
        return List.of();
    }

    private static void holdLock(CountDownLatch hasLock, CountDownLatch mayFinish) {
        hasLock.countDown();
        try {
            mayFinish.await(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

}
