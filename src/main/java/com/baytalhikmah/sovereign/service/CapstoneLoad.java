package com.baytalhikmah.sovereign.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The capstone load test: many shoppers check out at the same time; the report gives throughput, latency
 * percentiles and every failure. The numbers belong to the machine they were measured on.
 */
public class CapstoneLoad {

    private final CheckoutFlow checkout;

    public CapstoneLoad(CheckoutFlow checkout) {
        this.checkout = checkout;
    }

    public record Report(int checkouts, int failures, List<String> failureKinds, Duration wallTime,
            double throughputPerSecond, Duration p50, Duration p95) {
    }

    /**
     * Runs {@code checkouts} checkouts with {@code shoppers} threads. Checkout n buys one unit of
     * skus[n % skus.size()] for customer emails[n % emails.size()] with order number prefix + n.
     */
    public Report run(int shoppers, int checkouts, String orderPrefix, List<String> emails, List<String> skus) {
        ConcurrentLinkedQueue<Long> nanos = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
        AtomicInteger next = new AtomicInteger();
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(shoppers)) {
            for (int shopper = 0; shopper < shoppers; shopper++) {
                pool.submit(() -> {
                    for (int n = next.getAndIncrement(); n < checkouts; n = next.getAndIncrement()) {
                        long begin = System.nanoTime();
                        try {
                            checkout.checkout(orderPrefix + String.format("%04d", n), emails.get(n % emails.size()),
                                skus.get(n % skus.size()), 1);
                            nanos.add(System.nanoTime() - begin);
                        }
                        catch (RuntimeException failure) {
                            failures.add(failure.getClass().getSimpleName());
                        }
                    }
                });
            }
        }
        Duration wall = Duration.ofNanos(System.nanoTime() - start);
        List<Long> sorted = new ArrayList<>(nanos);
        Collections.sort(sorted);
        return new Report(sorted.size(), failures.size(), List.copyOf(failures), wall,
            sorted.size() / (wall.toNanos() / 1e9), percentile(sorted, 50), percentile(sorted, 95));
    }

    private static Duration percentile(List<Long> sorted, int percent) {
        if (sorted.isEmpty()) {
            return Duration.ZERO;
        }
        int index = (int) Math.ceil(percent / 100.0 * sorted.size()) - 1;
        return Duration.ofNanos(sorted.get(Math.max(0, index)));
    }

}
