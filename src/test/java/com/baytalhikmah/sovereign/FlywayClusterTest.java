package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.baytalhikmah.sovereign.domain.ProcessingActivity;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayClusterTest {

    private static final int NODES = 3;

    @Autowired
    Flyway flyway;

    @Autowired
    PostgreSQLContainer postgres;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @Test
    void threeNodesStartTogetherAndTheSchemaIsMigratedExactlyOnce() throws Exception {
        int migrations = (int) Arrays.stream(flyway.info().applied()).filter(m -> m.getVersion() != null).count();
        jdbc.execute("create database lab94_cluster");
        String url = postgres.getJdbcUrl().replace("/sovereign", "/lab94_cluster");

        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<MigrateResult>> nodes = new ArrayList<>();
        try (ExecutorService pods = Executors.newFixedThreadPool(NODES)) {
            for (int node = 1; node <= NODES; node++) {
                nodes.add(pods.submit(() -> {
                    startTogether.await();
                    return Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .dataSource(url, postgres.getUsername(), postgres.getPassword())
                        .load()
                        .migrate();
                }));
            }
            startTogether.countDown();
        }
        List<Integer> executed = new ArrayList<>();
        for (Future<MigrateResult> node : nodes) {
            executed.add(node.get().migrationsExecuted);
        }

        JdbcTemplate cluster = new JdbcTemplate(
            new SingleConnectionDataSource(url, postgres.getUsername(), postgres.getPassword(), true));
        List<Map<String, Object>> history = cluster.queryForList("""
                select version, description, success from compliance.flyway_schema_history
                where version is not null order by installed_rank""");
        Integer activities = cluster.queryForObject("select count(*) from compliance.sov_processing_activity",
            Integer.class);
        List<String> hibernateSees = entityManager
            .createQuery("select p.code from ProcessingActivity p order by p.code", String.class)
            .getResultList();
        ((SingleConnectionDataSource) cluster.getDataSource()).destroy();

        System.out.println("=== FLYWAY ON " + NODES + " NODES ===");
        for (int i = 0; i < NODES; i++) {
            System.out.println("node " + (i + 1) + " applied : " + executed.get(i) + " migration(s)");
        }
        history.forEach(row -> System.out.println("history      : V" + row.get("version") + " "
                + row.get("description") + " success=" + row.get("success")));
        System.out.println("activities   : " + activities + " rows");
        System.out.println("app database : " + hibernateSees);

        assertThat(executed).containsExactlyInAnyOrder(migrations, 0, 0);
        assertThat(history).hasSize(migrations).allMatch(row -> Boolean.TRUE.equals(row.get("success")));
        assertThat(activities).isEqualTo(3);
        assertThat(hibernateSees).contains("FRAUD_SCREENING", "MARKETING_EMAIL", "ORDER_FULFILMENT");
        assertThat(entityManager.find(ProcessingActivity.class, "MARKETING_EMAIL").getLegalBasis())
            .isEqualTo("CONSENT");
    }

}
