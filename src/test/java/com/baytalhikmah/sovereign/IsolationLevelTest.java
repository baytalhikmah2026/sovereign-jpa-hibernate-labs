package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.StockMovement;
import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.StockReservations;
import com.baytalhikmah.sovereign.service.StockReservations.TwoReadings;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class IsolationLevelTest {

    @Autowired
    StockReservations reservations;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void isolationLevelsDecideWhatATransactionSees() throws Exception {
        TransactionTemplate colleague = new TransactionTemplate(transactionManager);
        colleague.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        colleague.executeWithoutResult(status -> {
            entityManager.persist(new StockMovement("Q61-A", 10, "delivery"));
            entityManager.persist(new StockMovement("Q61-B", 10, "delivery"));
            entityManager.persist(new StockMovement("Q61-C", 10, "delivery"));
        });
        Runnable colleagueDeliversFive = () -> colleague.executeWithoutResult(status ->
            entityManager.persist(new StockMovement("Q61-A", 5, "delivery")));

        TwoReadings readCommitted = reservations.balanceTwice(
            "Q61-A", TransactionDefinition.ISOLATION_READ_COMMITTED, colleagueDeliversFive);
        TwoReadings repeatableRead = reservations.balanceTwice(
            "Q61-A", TransactionDefinition.ISOLATION_REPEATABLE_READ, colleagueDeliversFive);

        List<String> repeatableReadShipments = twoClerksShipEight("Q61-B", TransactionDefinition.ISOLATION_REPEATABLE_READ);
        List<String> serializableShipments = twoClerksShipEight("Q61-C", TransactionDefinition.ISOLATION_SERIALIZABLE);

        System.out.println("=== ISOLATION LEVELS ===");
        System.out.println("READ COMMITTED  : balance read twice = " + readCommitted.first() + ", " + readCommitted.second());
        System.out.println("REPEATABLE READ : balance read twice = " + repeatableRead.first() + ", " + repeatableRead.second());
        System.out.println("REPEATABLE READ : two clerks ship 8 of 10 -> " + repeatableReadShipments
            + ", balance " + reservations.balance("Q61-B"));
        System.out.println("SERIALIZABLE    : two clerks ship 8 of 10 -> " + serializableShipments
            + ", balance " + reservations.balance("Q61-C"));

        assertThat(readCommitted).isEqualTo(new TwoReadings(10, 15));
        assertThat(repeatableRead).isEqualTo(new TwoReadings(15, 15));
        assertThat(reservations.balance("Q61-A")).isEqualTo(20);
        assertThat(repeatableReadShipments).containsExactly("shipped", "shipped");
        assertThat(reservations.balance("Q61-B")).isEqualTo(-6);
        assertThat(serializableShipments).containsExactlyInAnyOrder("shipped", "failed 40001");
        assertThat(reservations.balance("Q61-C")).isEqualTo(2);
    }

    /** Two clerks check the balance at the same moment, then each ships 8. Returns what happened to each. */
    private List<String> twoClerksShipEight(String sku, int isolationLevel) throws Exception {
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        Runnable waitForTheOtherClerk = () -> {
            try {
                bothChecked.await(10, TimeUnit.SECONDS);
            }
            catch (Exception notBoth) {
                throw new IllegalStateException(notBoth);
            }
        };
        ExecutorService clerks = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            results.add(clerks.submit(() -> reservations.shipIfAvailable(sku, 8, isolationLevel, waitForTheOtherClerk)));
        }
        List<String> outcomes = new ArrayList<>();
        for (Future<Boolean> result : results) {
            try {
                outcomes.add(result.get(30, TimeUnit.SECONDS) ? "shipped" : "refused");
            }
            catch (ExecutionException failed) {
                outcomes.add("failed " + SqlStates.of(failed.getCause()));
            }
        }
        clerks.shutdown();
        return outcomes;
    }

}
