package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import({ TestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@ActiveProfiles("cache")
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SecondLevelCacheTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    @Qualifier("redisContainer")
    GenericContainer<?> redis;

    /** What one read in a fresh transaction found, and what it cost. */
    record Reading(String price, long statements, long hits, long misses) {
    }

    @Test
    void repeatedReadsComeFromRedis() throws Exception {
        UUID kettleId = transaction.execute(status -> {
            Product kettle = new Product("Q62-KET", "Copper Kettle", Money.of("45.00", "GBP"));
            entityManager.persist(kettle);
            return kettle.getId();
        });
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManagerFactory.getCache().evictAll();
        statistics.clear();

        Reading first = read(kettleId, statistics);
        Reading second = read(kettleId, statistics);
        transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, kettleId).changePrice(Money.of("49.00", "GBP")));
        Reading afterPriceChange = read(kettleId, statistics);
        String redisKeys = redis.execInContainer("redis-cli", "--scan").getStdout();

        System.out.println("=== SECOND-LEVEL CACHE (Redis) ===");
        System.out.println("First read          : " + first);
        System.out.println("Second read         : " + second);
        System.out.println("After price change  : " + afterPriceChange);
        System.out.println("Cache puts          : " + statistics.getSecondLevelCachePutCount());
        System.out.println("Keys in Redis       :");
        System.out.print(redisKeys);

        assertThat(first).isEqualTo(new Reading("45.00 GBP", 1, 0, 1));
        assertThat(second).isEqualTo(new Reading("45.00 GBP", 0, 1, 0));
        assertThat(afterPriceChange).isEqualTo(new Reading("49.00 GBP", 0, 1, 0));
        assertThat(redisKeys).contains("com.baytalhikmah.sovereign.domain.Product");
    }

    private Reading read(UUID productId, Statistics statistics) {
        long statementsBefore = statistics.getPrepareStatementCount();
        long hitsBefore = statistics.getSecondLevelCacheHitCount();
        long missesBefore = statistics.getSecondLevelCacheMissCount();
        String price = transaction.execute(status -> entityManager.find(Product.class, productId).getPrice().toString());
        return new Reading(price,
            statistics.getPrepareStatementCount() - statementsBefore,
            statistics.getSecondLevelCacheHitCount() - hitsBefore,
            statistics.getSecondLevelCacheMissCount() - missesBefore);
    }

}
