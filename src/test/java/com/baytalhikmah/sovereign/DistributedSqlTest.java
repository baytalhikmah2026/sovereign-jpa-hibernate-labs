package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.cockroachdb.CockroachContainer;

import com.baytalhikmah.sovereign.persistence.GlobalPersistenceUnit;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;
import com.baytalhikmah.sovereign.service.GlobalStockDesk;

class DistributedSqlTest {

    @Test
    void conflictingReservationsAreRetriedInsteadOfLost() throws Exception {
        try (CockroachContainer cockroach = new CockroachContainer("cockroachdb/cockroach:v26.3.5")) {
            cockroach.start();
            try (GlobalPersistenceUnit global = new GlobalPersistenceUnit(cockroach.getJdbcUrl(),
                    cockroach.getUsername(), cockroach.getPassword())) {
                TransactionRetry noRetry = new TransactionRetry(global.entityManagerFactory(), 1, Duration.ofMillis(50));
                TransactionRetry withRetry = new TransactionRetry(global.entityManagerFactory(), 5, Duration.ofMillis(50));
                GlobalStockDesk naive = new GlobalStockDesk(noRetry);
                GlobalStockDesk resilient = new GlobalStockDesk(withRetry);

                naive.register("Q97-LANTERN", "eu-west", "DE", 10);
                naive.register("Q97-LANTERN", "us-east", "US", 10);

                List<String> naiveOutcome = twoBuyersAtOnce(naive, "eu-west");
                List<String> resilientOutcome = twoBuyersAtOnce(resilient, "us-east");
                int euLeft = naive.quantity("Q97-LANTERN", "eu-west");
                int usLeft = resilient.quantity("Q97-LANTERN", "us-east");

                System.out.println("=== DISTRIBUTED SQL: CockroachDB ===");
                System.out.println("without retry : " + naiveOutcome + " -> eu-west left " + euLeft);
                System.out.println("with retry    : " + resilientOutcome + " -> us-east left " + usLeft
                        + " (retries: " + withRetry.retries() + ")");

                assertThat(naiveOutcome).containsExactlyInAnyOrder("reserved", "failed: retry error 40001");
                assertThat(euLeft).isEqualTo(9);
                assertThat(resilientOutcome).containsExactly("reserved", "reserved");
                assertThat(usLeft).isEqualTo(8);
                assertThat(withRetry.retries()).isPositive();
            }
        }
    }

    /** Two buyers read the same stock at the same moment, then both reserve one unit. */
    private static List<String> twoBuyersAtOnce(GlobalStockDesk desk, String region) throws InterruptedException {
        CountDownLatch bothHaveRead = new CountDownLatch(2);
        Runnable waitForTheOther = () -> {
            bothHaveRead.countDown();
            try {
                bothHaveRead.await(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
        List<Future<Integer>> buyers = new ArrayList<>();
        try (ExecutorService shoppers = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                buyers.add(shoppers.submit(() -> desk.reserve("Q97-LANTERN", region, 1, waitForTheOther)));
            }
        }
        List<String> outcome = new ArrayList<>();
        for (Future<Integer> buyer : buyers) {
            try {
                buyer.get();
                outcome.add("reserved");
            }
            catch (ExecutionException failure) {
                outcome.add(TransactionRetry.isRetryable(failure.getCause()) ? "failed: retry error 40001"
                        : "failed: " + failure.getCause());
            }
        }
        return outcome;
    }

}
