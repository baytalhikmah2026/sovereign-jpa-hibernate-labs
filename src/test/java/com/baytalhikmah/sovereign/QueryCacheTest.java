package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.TaxRate;
import com.baytalhikmah.sovereign.query.CachedQueries;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import({ TestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@ActiveProfiles("cache")
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class QueryCacheTest {

    private static final BigDecimal MIN = new BigDecimal("10.00");
    private static final BigDecimal MAX = new BigDecimal("20.00");

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    CachedQueries cachedQueries;

    /** What one query-cache region did during one phase. */
    record Tally(long puts, long hits, long misses) {
    }

    @Test
    void writesToTheTableThrowCachedResultsAway() {
        transaction.executeWithoutResult(status -> {
            for (int i = 1; i <= 10; i++) {
                entityManager.persist(new Product("Q64-P%02d".formatted(i), "Shelf item " + i,
                    Money.of((10 + i) + ".00", "GBP")));
            }
            entityManager.persist(new TaxRate("Q64-AE-STD", "AE", new BigDecimal("5.00"), LocalDate.of(2018, 1, 1)));
            entityManager.persist(new TaxRate("Q64-AE-ZERO", "AE", new BigDecimal("0.00"), LocalDate.of(2018, 1, 1)));
        });
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManagerFactory.getCache().evictAll();
        statistics.clear();

        Tally quietBefore = tally(statistics, CachedQueries.CATALOGUE_QUERIES);
        int found = 0;
        for (int i = 0; i < 20; i++) {
            found = transaction.execute(status -> cachedQueries.productsInPriceBand("Q64-P", MIN, MAX).size());
        }
        Tally quiet = minus(tally(statistics, CachedQueries.CATALOGUE_QUERIES), quietBefore);

        Tally busyBefore = tally(statistics, CachedQueries.CATALOGUE_QUERIES);
        for (int i = 1; i <= 20; i++) {
            String sku = "Q64-N%02d".formatted(i);
            transaction.executeWithoutResult(status ->
                entityManager.persist(new Product(sku, "New arrival", Money.of("99.00", "GBP"))));
            transaction.executeWithoutResult(status -> cachedQueries.productsInPriceBand("Q64-P", MIN, MAX));
            transaction.executeWithoutResult(status -> cachedQueries.taxRatesFor("AE"));
        }
        Tally busy = minus(tally(statistics, CachedQueries.CATALOGUE_QUERIES), busyBefore);
        Tally reference = tally(statistics, CachedQueries.REFERENCE_QUERIES);

        System.out.println("=== QUERY CACHE ===");
        System.out.println("Products found in band            : " + found);
        System.out.println("Quiet: 20 product queries         : " + quiet);
        System.out.println("Busy : 20 product queries + writes: " + busy);
        System.out.println("Busy : 20 tax-rate queries        : " + reference);
        System.out.printf("Hit ratio quiet / busy / tax      : %d%% / %d%% / %d%%%n",
            percent(quiet), percent(busy), percent(reference));

        assertThat(found).isEqualTo(10);
        assertThat(quiet).isEqualTo(new Tally(1, 19, 1));
        assertThat(busy).isEqualTo(new Tally(20, 0, 20));
        assertThat(reference).isEqualTo(new Tally(1, 19, 1));
    }

    private static Tally tally(Statistics statistics, String region) {
        CacheRegionStatistics regionStatistics = statistics.getQueryRegionStatistics(region);
        if (regionStatistics == null) {
            return new Tally(0, 0, 0);
        }
        return new Tally(regionStatistics.getPutCount(), regionStatistics.getHitCount(), regionStatistics.getMissCount());
    }

    private static Tally minus(Tally after, Tally before) {
        return new Tally(after.puts() - before.puts(), after.hits() - before.hits(), after.misses() - before.misses());
    }

    private static long percent(Tally tally) {
        return 100 * tally.hits() / (tally.hits() + tally.misses());
    }

}
