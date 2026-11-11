package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import({ TestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@ActiveProfiles("cache")
@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.properties.hibernate.cache.redisson.catalogue.expiration.time_to_live=1500" })
class CacheRegionTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    /** What one step cost: SQL statements, and cache hits and misses across all regions. */
    record Reading(long statements, long hits, long misses) {
    }

    @Test
    void eachRegionKeepsItsOwnRules() throws Exception {
        UUID kettleId = transaction.execute(status -> {
            Product kettle = new Product("Q63-KET", "Copper Kettle", Money.of("45.00", "GBP"));
            kettle.addImage("https://img.sovereign.test/q63-ket-front.jpg");
            kettle.addImage("https://img.sovereign.test/q63-ket-side.jpg");
            entityManager.persist(kettle);
            entityManager.persist(new TaxRate("Q63-GB-STD", "GB", new BigDecimal("20.00"), LocalDate.of(2011, 1, 4)));
            return kettle.getId();
        });
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManagerFactory.getCache().evictAll();
        statistics.clear();

        Consumer<EntityManager> productWithImages = em -> em.find(Product.class, kettleId).getImageUrls().size();
        Consumer<EntityManager> taxRate = em -> em.find(TaxRate.class, "Q63-GB-STD").getRatePercent();

        Reading productFirst = read(productWithImages, statistics);
        Reading productSecond = read(productWithImages, statistics);
        Reading taxFirst = read(taxRate, statistics);
        Reading taxSecond = read(taxRate, statistics);
        Thread.sleep(2000);
        Reading productAfterTtl = read(productWithImages, statistics);
        Reading taxAfterTwoSeconds = read(taxRate, statistics);

        List<String> regions = List.of("catalogue", "catalogue_images", "reference");

        System.out.println("=== CACHE REGIONS ===");
        System.out.println("Product + images, 1st    : " + productFirst);
        System.out.println("Product + images, 2nd    : " + productSecond);
        System.out.println("Tax rate, 1st            : " + taxFirst);
        System.out.println("Tax rate, 2nd            : " + taxSecond);
        System.out.println("-- 2 seconds later --");
        System.out.println("Product + images         : " + productAfterTtl);
        System.out.println("Tax rate                 : " + taxAfterTwoSeconds);
        for (String region : regions) {
            CacheRegionStatistics regionStatistics = statistics.getDomainDataRegionStatistics(region);
            System.out.printf("%-16s puts=%d hits=%d misses=%d%n", region,
                regionStatistics.getPutCount(), regionStatistics.getHitCount(), regionStatistics.getMissCount());
        }

        assertThat(statistics.getSecondLevelCacheRegionNames()).contains("catalogue", "catalogue_images", "reference");
        assertThat(productFirst).isEqualTo(new Reading(2, 0, 2));
        assertThat(productSecond).isEqualTo(new Reading(0, 2, 0));
        assertThat(taxFirst).isEqualTo(new Reading(1, 0, 1));
        assertThat(taxSecond).isEqualTo(new Reading(0, 1, 0));
        assertThat(productAfterTtl).isEqualTo(new Reading(1, 1, 1));
        assertThat(taxAfterTwoSeconds).isEqualTo(new Reading(0, 1, 0));
    }

    private Reading read(Consumer<EntityManager> work, Statistics statistics) {
        long statementsBefore = statistics.getPrepareStatementCount();
        long hitsBefore = statistics.getSecondLevelCacheHitCount();
        long missesBefore = statistics.getSecondLevelCacheMissCount();
        transaction.executeWithoutResult(status -> work.accept(entityManager));
        return new Reading(
            statistics.getPrepareStatementCount() - statementsBefore,
            statistics.getSecondLevelCacheHitCount() - hitsBefore,
            statistics.getSecondLevelCacheMissCount() - missesBefore);
    }

}
