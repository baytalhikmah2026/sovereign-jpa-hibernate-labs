package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.baytalhikmah.sovereign.query.OrderQueries;
import com.baytalhikmah.sovereign.service.OrderOverview;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class NPlusOneTrafficTest {

    static final String DOMAIN = QueryLabData.emailDomain("q41");

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Autowired
    OrderQueries orderQueries;

    @Autowired
    PostgreSQLContainer postgres;

    @Test
    void theNetworkShowsEveryLazyLoad() throws Exception {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q41"));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        try (GenericContainer<?> sniffer = new GenericContainer<>(DockerImageName.parse("nicolaka/netshoot:v0.16"))
                .withNetworkMode("container:" + postgres.getContainerId())
                .withCommand("tcpdump", "-i", "any", "-nn", "-l", "-q",
                    "tcp dst port 5432 and (tcp[tcpflags] & tcp-push != 0)")
                .waitingFor(Wait.forLogMessage(".*listening on.*", 1))) {
            sniffer.start();

            statistics.clear();
            List<String> overview = orderOverview.naiveOverview(DOMAIN);
            long naiveStatements = statistics.getPrepareStatementCount();
            long naivePackets = packetsSoFar(sniffer);

            statistics.clear();
            orderQueries.summariesFor(DOMAIN);
            long projectionStatements = statistics.getPrepareStatementCount();
            long projectionPackets = packetsSoFar(sniffer) - naivePackets;

            System.out.println("=== N+1 ON THE WIRE ===");
            overview.forEach(line -> System.out.println("  " + line));
            System.out.println("Lazy overview : " + naiveStatements + " SQL statements, " + naivePackets + " request packets to PostgreSQL");
            System.out.println("Projection    : " + projectionStatements + " SQL statement,  " + projectionPackets + " request packet(s) to PostgreSQL");

            assertThat(overview).hasSize(6).first().isEqualTo("Q41-A1 | Amina Yusuf | 2 lines");
            assertThat(naiveStatements).isEqualTo(10);
            assertThat(projectionStatements).isEqualTo(1);
            assertThat(naivePackets).isGreaterThanOrEqualTo(10).isGreaterThan(projectionPackets);
        }
    }

    /** Packets seen by tcpdump so far: one line per request packet sent to port 5432. */
    private static long packetsSoFar(GenericContainer<?> sniffer) throws InterruptedException {
        Thread.sleep(1000);
        return sniffer.getLogs(OutputFrame.OutputType.STDOUT).lines()
            .filter(line -> line.contains(".5432:"))
            .count();
    }

}
