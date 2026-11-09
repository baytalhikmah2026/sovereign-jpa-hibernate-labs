package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.TaxRate;
import com.baytalhikmah.sovereign.service.CustomerSeedService;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class ImmutableEntityTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    CustomerSeedService seedService;

    @Test
    void immutableReferenceDataIsNeverTracked() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new TaxRate("GB-STANDARD", "GB", new BigDecimal("20.00"), LocalDate.of(2011, 1, 4)));
            entityManager.persist(new TaxRate("GB-REDUCED", "GB", new BigDecimal("5.00"), LocalDate.of(1997, 9, 1)));
            entityManager.persist(new TaxRate("PK-STANDARD", "PK", new BigDecimal("18.00"), LocalDate.of(2025, 7, 1)));
        });

        transaction.executeWithoutResult(status -> {
            TaxRate standard = entityManager.find(TaxRate.class, "GB-STANDARD");
            boolean readOnly = entityManager.unwrap(Session.class).isReadOnly(standard);

            System.out.println("=== IMMUTABLE ENTITY ===");
            System.out.println("Tax on 79.00 GBP     : " + standard.taxOn(Money.of("79.00", "GBP")));
            System.out.println("Tracked as read-only : " + readOnly);

            assertThat(readOnly).isTrue();
            assertThat(standard.taxOn(Money.of("79.00", "GBP"))).isEqualTo(Money.of("15.80", "GBP"));
        });

        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                entityManager.createQuery("update TaxRate t set t.ratePercent = 25 where t.code = 'GB-STANDARD'")
                    .executeUpdate()))
            .hasMessageContaining("immutable");
        System.out.println("Bulk update attempt  : refused");
    }

    @Test
    void readOnlyQueriesSkipTheDirtyCheck() {
        seedService.deleteSeedCustomers();
        seedService.insert(CustomerSeedService.generate(5_000, CustomerSeedService.SEED), 50);
        try {
            long tracked = timeLoadAndFlush(false);
            long readOnly = timeLoadAndFlush(true);

            System.out.println("=== 5,000 CUSTOMERS: LOAD + FLUSH ===");
            System.out.println("Normal (tracked)     : " + tracked + " ms");
            System.out.println("Read-only hint       : " + readOnly + " ms");
        }
        finally {
            seedService.deleteSeedCustomers();
        }
    }

    private long timeLoadAndFlush(boolean readOnly) {
        return transaction.execute(status -> {
            long start = System.nanoTime();
            List<Customer> customers = entityManager.createQuery(
                    "select c from Customer c where c.email like '%@seed.sovereign.test'", Customer.class)
                .setHint(HibernateHints.HINT_READ_ONLY, readOnly)
                .getResultList();
            entityManager.flush();
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertThat(customers).hasSize(5_000);
            assertThat(entityManager.unwrap(Session.class).isReadOnly(customers.get(0))).isEqualTo(readOnly);
            entityManager.clear();
            return millis;
        });
    }

}
