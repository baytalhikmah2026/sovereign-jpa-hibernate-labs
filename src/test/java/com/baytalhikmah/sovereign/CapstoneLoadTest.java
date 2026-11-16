package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.cockroachdb.CockroachContainer;

import com.baytalhikmah.sovereign.audit.AuditChainVerifier;
import com.baytalhikmah.sovereign.audit.TamperEvidentLog;
import com.baytalhikmah.sovereign.config.CapstoneRunner;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.GlobalPersistenceUnit;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;
import com.baytalhikmah.sovereign.service.CapstoneLoad;
import com.baytalhikmah.sovereign.service.CheckoutFlow;
import com.baytalhikmah.sovereign.service.GlobalStockDesk;
import com.baytalhikmah.sovereign.service.OrderDesk;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CapstoneLoadTest {

    private static final int SHOPPERS = 8;

    private static final int CHECKOUTS = 320;

    @Autowired
    OrderDesk orders;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    TamperEvidentLog writeLog;

    @Test
    void theWholeGridTakesTheLoadAndKeepsEveryNumberRight() throws Exception {
        List<String> emails = new ArrayList<>();
        List<String> skus = new ArrayList<>();
        transaction.executeWithoutResult(status -> {
            for (int i = 0; i < 20; i++) {
                Customer buyer = new Customer("Capstone Buyer " + i, "buyer" + i + "@q100.shop.test");
                entityManager.persist(buyer);
                emails.add(buyer.getEmail());
            }
            for (int i = 0; i < 8; i++) {
                entityManager.persist(new Product("Q100-P" + i, "Capstone product " + i, Money.of("25.00", "EUR")));
                skus.add("Q100-P" + i);
            }
        });

        try (CockroachContainer cockroach = new CockroachContainer("cockroachdb/cockroach:v26.3.5")) {
            cockroach.start();
            try (GlobalPersistenceUnit global = new GlobalPersistenceUnit(cockroach.getJdbcUrl(),
                    cockroach.getUsername(), cockroach.getPassword())) {
                GlobalStockDesk stock = new GlobalStockDesk(
                    new TransactionRetry(global.entityManagerFactory(), 5, Duration.ofMillis(20)));
                for (String sku : skus) {
                    stock.ensureStock(sku, "eu-west", "DE", 1_000);
                }
                CheckoutFlow checkout = new CheckoutFlow(stock, orders, "eu-west");

                Throwable unknownBuyer = catchThrowable(() ->
                    checkout.checkout("Q100-X001", "nobody@q100.shop.test", "Q100-P0", 1));
                int afterCompensation = stock.quantity("Q100-P0", "eu-west");

                CapstoneLoad.Report report =
                    new CapstoneLoad(checkout).run(SHOPPERS, CHECKOUTS, "Q100-", emails, skus);

                int unitsLeft = 0;
                for (String sku : skus) {
                    unitsLeft += stock.quantity(sku, "eu-west");
                }
                int unitsSold = 8 * 1_000 - unitsLeft;
                Integer outboxEvents = jdbc.queryForObject(
                    "select count(*) from sov_outbox_event where aggregate_id like 'Q100-0%'", Integer.class);
                int active = dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
                boolean chainIntact = AuditChainVerifier.verify(Files.readAllLines(writeLog.segment())).intact();

                System.out.println("=== CAPSTONE: SOVEREIGN GLOBAL DATA GRID ===");
                System.out.println("compensation       : order failed (" + unknownBuyer.getClass().getSimpleName()
                        + "), stock back to " + afterCompensation);
                CapstoneRunner.print(report, SHOPPERS, unitsSold, active, chainIntact);
                System.out.println("outbox events      : " + outboxEvents);

                assertThat(unknownBuyer).isNotNull();
                assertThat(afterCompensation).isEqualTo(1_000);
                assertThat(report.failures()).isZero();
                assertThat(report.checkouts()).isEqualTo(CHECKOUTS);
                assertThat(unitsSold).isEqualTo(CHECKOUTS);
                assertThat(outboxEvents).isEqualTo(CHECKOUTS);
                assertThat(active).isZero();
                assertThat(chainIntact).isTrue();
                assertThat(report.throughputPerSecond()).isPositive();
            }
        }
    }

}
