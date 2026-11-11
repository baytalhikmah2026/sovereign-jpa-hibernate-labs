package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.CacheInvalidationListener;
import com.baytalhikmah.sovereign.service.ExternalPriceFeed;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import({ TestcontainersConfiguration.class, RedisTestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class })
@ActiveProfiles({ "cache", "invalidation" })
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CacheInvalidationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    ExternalPriceFeed priceFeed;

    @Autowired
    CacheInvalidationListener listener;

    /** The price one read returned, and how many SQL statements it needed. */
    record Reading(String price, long statements) {
    }

    @Test
    void aMessageEvictsTheStaleProduct() throws Exception {
        UUID kettleId = transaction.execute(status -> {
            Product kettle = new Product("Q65-KET", "Copper Kettle", Money.of("45.00", "GBP"));
            entityManager.persist(kettle);
            return kettle.getId();
        });
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManagerFactory.getCache().evictAll();

        Reading cached = read(kettleId, statistics);
        priceFeed.updatePrice("Q65-KET", new BigDecimal("47.00"));
        Reading afterSilentChange = read(kettleId, statistics);
        priceFeed.announcePriceChange(kettleId, "Q65-KET");
        long waitedMillis = waitForEvictions(1);
        Reading afterMessage = read(kettleId, statistics);

        System.out.println("=== CACHE INVALIDATION (Kafka) ===");
        System.out.println("Before the change         : " + cached);
        System.out.println("After a change in SQL     : " + afterSilentChange);
        System.out.println("After the Kafka message   : " + afterMessage);
        System.out.println("Evictions                 : " + listener.evictionCount());
        System.out.println("Message to eviction       : under " + (waitedMillis / 1000 + 1) + " s");

        assertThat(cached).isEqualTo(new Reading("45.00 GBP", 1));
        assertThat(afterSilentChange).isEqualTo(new Reading("45.00 GBP", 0));
        assertThat(afterMessage).isEqualTo(new Reading("47.00 GBP", 1));
        assertThat(listener.evictionCount()).isEqualTo(1);
    }

    private Reading read(UUID productId, Statistics statistics) {
        long before = statistics.getPrepareStatementCount();
        String price = transaction.execute(status -> entityManager.find(Product.class, productId).getPrice().toString());
        return new Reading(price, statistics.getPrepareStatementCount() - before);
    }

    private long waitForEvictions(long expected) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (listener.evictionCount() < expected && System.currentTimeMillis() - start < 30_000) {
            Thread.sleep(100);
        }
        return System.currentTimeMillis() - start;
    }

}
