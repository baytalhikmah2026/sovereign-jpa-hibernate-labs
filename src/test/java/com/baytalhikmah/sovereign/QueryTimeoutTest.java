package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.QueryTimeoutException;

import com.baytalhikmah.sovereign.query.SlowQueries;
import com.zaxxer.hikari.HikariDataSource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class QueryTimeoutTest {

    @Autowired
    SlowQueries slowQueries;

    @Autowired
    DataSource dataSource;

    @Test
    void runawayQueriesAreCancelledAndThePoolRecovers() throws Exception {
        Outcome perQuery = attempt(() -> slowQueries.runWithQueryTimeout(3, 1000));
        Outcome perTransaction = attempt(() -> slowQueries.runInShortTransaction(3));
        Outcome byDefault = attempt(() -> slowQueries.runWithDefaultTimeout(1));
        int activeConnections = dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();

        System.out.println("=== QUERY TIMEOUTS ===");
        System.out.println("3 s query, 1 s query timeout       : " + perQuery);
        System.out.println("3 s query, 1 s transaction timeout : " + perTransaction);
        System.out.println("1 s query, 5 s default timeout     : " + byDefault);
        System.out.println("Active connections afterwards      : " + activeConnections);

        assertThat(perQuery.error()).isInstanceOf(QueryTimeoutException.class);
        assertThat(perQuery.millis()).isLessThan(2500);
        assertThat(perTransaction.error()).isNotNull();
        assertThat(perTransaction.millis()).isLessThan(2500);
        assertThat(byDefault.error()).isNull();
        assertThat(activeConnections).isZero();
    }

    private static Outcome attempt(Supplier<Object> work) {
        long start = System.nanoTime();
        try {
            work.get();
            return new Outcome(null, (System.nanoTime() - start) / 1_000_000);
        } catch (RuntimeException e) {
            return new Outcome(e, (System.nanoTime() - start) / 1_000_000);
        }
    }

    record Outcome(RuntimeException error, long millis) {
        @Override
        public String toString() {
            return (error == null ? "finished" : "stopped by " + error.getClass().getSimpleName()) + " after " + millis + " ms";
        }
    }

}
