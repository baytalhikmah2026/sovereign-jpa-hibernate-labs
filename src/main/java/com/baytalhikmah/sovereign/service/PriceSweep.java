package com.baytalhikmah.sovereign.service;

import java.lang.management.ManagementFactory;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Walks through a large part of the catalogue and raises every price, with or without clearing the persistence context. */
@Service
public class PriceSweep {

    /** Products changed, most entities held at once, time spent flushing, and heap in use at the busiest moment. */
    public record SweepReport(int processed, int peakManaged, long flushMillis, long peakHeapMegabytes) {
    }

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Adds {@code increase} to the price of every product whose SKU starts with {@code skuPrefix}.
     * {@code clearEvery} = 0 keeps every product managed until the end; a positive number flushes and clears after that many.
     */
    @Transactional
    public SweepReport raisePrices(String skuPrefix, Money increase, int clearEvery) {
        Session session = entityManager.unwrap(Session.class);
        int processed = 0;
        int peakManaged = 0;
        long flushNanos = 0;
        long peakHeap = 0;
        try (Stream<Product> products = entityManager.createQuery(
                "select p from Product p where p.sku like :prefix order by p.sku", Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .setHint(HibernateHints.HINT_FETCH_SIZE, 500)
            .getResultStream()) {
            for (Product product : (Iterable<Product>) products::iterator) {
                product.changePrice(product.getPrice().plus(increase));
                processed++;
                peakManaged = Math.max(peakManaged, session.getStatistics().getEntityCount());
                if (clearEvery > 0 && processed % clearEvery == 0) {
                    peakHeap = Math.max(peakHeap, heapInUse());
                    long start = System.nanoTime();
                    entityManager.flush();
                    flushNanos += System.nanoTime() - start;
                    entityManager.clear();
                }
            }
        }
        peakHeap = Math.max(peakHeap, heapInUse());
        long start = System.nanoTime();
        entityManager.flush();
        flushNanos += System.nanoTime() - start;
        entityManager.clear();
        return new SweepReport(processed, peakManaged, flushNanos / 1_000_000, peakHeap / (1024 * 1024));
    }

    private static long heapInUse() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

}
