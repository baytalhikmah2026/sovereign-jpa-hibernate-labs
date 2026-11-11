package com.baytalhikmah.sovereign.persistence;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.query.CachedQueries;

import jakarta.persistence.EntityManagerFactory;

/** Evicts cached products when a price-change message arrives. Every application copy gets every message. */
@Component
@Profile("invalidation")
public class CacheInvalidationListener {

    public static final String PRICE_CHANGES = "catalogue.price-changes";

    private final EntityManagerFactory entityManagerFactory;
    private final AtomicLong evictions = new AtomicLong();

    public CacheInvalidationListener(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @KafkaListener(topics = PRICE_CHANGES,
            groupId = "#{'sovereign-cache-' + T(java.util.UUID).randomUUID()}",
            properties = "auto.offset.reset=earliest")
    public void onPriceChange(ConsumerRecord<String, String> message) {
        UUID productId = UUID.fromString(message.key());
        entityManagerFactory.getCache().evict(Product.class, productId);
        entityManagerFactory.getCache().unwrap(org.hibernate.Cache.class).evictQueryRegion(CachedQueries.CATALOGUE_QUERIES);
        long total = evictions.incrementAndGet();
        System.out.println("[CACHE] evicted product " + productId + " (" + message.value() + "), evictions so far: " + total);
    }

    public long evictionCount() {
        return evictions.get();
    }

}
