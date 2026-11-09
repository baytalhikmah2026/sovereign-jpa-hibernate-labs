package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConnectionPoolTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    PrometheusMeterRegistry prometheus;

    @Test
    void poolFollowsTheLittlesLawPlan() throws Exception {
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);

        System.out.println("=== CONNECTION POOL ===");
        System.out.println("Pool name       : " + pool.getPoolName());
        System.out.println("Maximum size    : " + pool.getMaximumPoolSize());
        System.out.println("Minimum idle    : " + pool.getMinimumIdle());
        System.out.println("Wait limit (ms) : " + pool.getConnectionTimeout());
        System.out.println("Leak alarm (ms) : " + pool.getLeakDetectionThreshold());

        assertThat(pool.getPoolName()).isEqualTo("SovereignPool");
        assertThat(pool.getMaximumPoolSize()).isEqualTo(10);
        assertThat(pool.getLeakDetectionThreshold()).isEqualTo(2000);
    }

    @Test
    void poolPublishesLiveMetrics() {
        String metrics = prometheus.scrape();

        System.out.println("=== POOL METRICS (Prometheus format) ===");
        metrics.lines()
            .filter(line -> line.startsWith("hikaricp_connections"))
            .filter(line -> !line.contains("_seconds"))
            .forEach(System.out::println);

        assertThat(metrics).contains("hikaricp_connections_max{pool=\"SovereignPool\"} 10.0");
    }

    @Test
    void leakDetectorRaisesTheAlarm() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("=== Holding a connection for 3 seconds ===");
            Thread.sleep(3000);
        }
        System.out.println("=== Connection returned ===");
    }

    @Test
    void emptyPoolMakesTheEleventhCallerWait() throws Exception {
        List<Connection> borrowed = new ArrayList<>();
        try {
            for (int i = 1; i <= 10; i++) {
                borrowed.add(dataSource.getConnection());
            }
            System.out.println("=== All 10 connections borrowed. Asking for number 11... ===");
            assertThatThrownBy(dataSource::getConnection)
                .isInstanceOf(SQLTransientConnectionException.class)
                .hasMessageContaining("request timed out after")
                .satisfies(error -> System.out.println("Pool answered: " + error.getMessage()));
        }
        finally {
            for (Connection connection : borrowed) {
                connection.close();
            }
        }
    }

}
