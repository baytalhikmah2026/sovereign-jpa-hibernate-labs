package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.ReplicaProbe;
import com.baytalhikmah.sovereign.service.ShopCustomers;

import jakarta.persistence.EntityManager;

@Import(ReplicaTestcontainersConfiguration.class)
@ActiveProfiles("replica")
@SpringBootTest
class ReplicaRoutingTest {

    @Autowired
    ReplicaProbe replicaProbe;

    @Autowired
    ShopCustomers shopCustomers;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    @Qualifier("primaryPool")
    DataSource primaryPool;

    @Autowired
    @Qualifier("replicaPool")
    DataSource replicaPool;

    @Test
    void readOnlyTransactionsGoToTheReplica() throws Exception {
        boolean writeOnReplica = replicaProbe.writeTransactionRunsOnReplica();
        boolean readOnReplica = replicaProbe.readTransactionRunsOnReplica();

        shopCustomers.register("Rania Odeh", "rania@replica.test");
        long lagMillis = waitUntilReplicaCaughtUp(Duration.ofSeconds(10));
        List<String> namesFromReplica = shopCustomers.names();

        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        Throwable writeInReadOnly = catchThrowable(() -> readOnly.executeWithoutResult(status -> {
            entityManager.persist(new Customer("Sami Jaber", "sami@replica.test"));
            entityManager.flush();
        }));

        System.out.println("=== READ/WRITE SPLIT ===");
        System.out.println("write transaction on replica : " + writeOnReplica);
        System.out.println("read transaction on replica  : " + readOnReplica);
        System.out.println("replica caught up after      : about " + lagMillis + " ms");
        System.out.println("names read from the replica  : " + namesFromReplica);
        System.out.println("write in read-only transaction: SQLSTATE " + SqlStates.of(writeInReadOnly));

        assertThat(writeOnReplica).isFalse();
        assertThat(readOnReplica).isTrue();
        assertThat(namesFromReplica).contains("Rania Odeh");
        assertThat(SqlStates.of(writeInReadOnly)).isEqualTo("25006");
    }

    /** Waits until the replica has replayed everything the primary has written so far; returns the wait. */
    private long waitUntilReplicaCaughtUp(Duration limit) throws InterruptedException {
        Instant start = Instant.now();
        String primaryPosition = new JdbcTemplate(primaryPool).queryForObject(
            "select pg_current_wal_lsn()::text", String.class);
        JdbcTemplate replica = new JdbcTemplate(replicaPool);
        while (!Boolean.TRUE.equals(replica.queryForObject(
                "select pg_last_wal_replay_lsn() >= ?::pg_lsn", Boolean.class, primaryPosition))) {
            if (Duration.between(start, Instant.now()).compareTo(limit) > 0) {
                throw new IllegalStateException("The replica did not catch up within " + limit);
            }
            Thread.sleep(20);
        }
        return Duration.between(start, Instant.now()).toMillis();
    }

}
