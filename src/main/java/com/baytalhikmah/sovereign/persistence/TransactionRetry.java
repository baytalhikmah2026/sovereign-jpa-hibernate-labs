package com.baytalhikmah.sovereign.persistence;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Runs a unit of work in a transaction and repeats it when the database asks for a retry (SQLSTATE 40001, sent by
 * CockroachDB when two transactions conflict). Waits grow exponentially, with random jitter so that the competing
 * transactions do not collide again at the same moment.
 */
public class TransactionRetry {

    static final String RETRY_SQL_STATE = "40001";

    private final EntityManagerFactory entityManagerFactory;

    private final int maxAttempts;

    private final Duration firstWait;

    private final AtomicLong retries = new AtomicLong();

    public TransactionRetry(EntityManagerFactory entityManagerFactory, int maxAttempts, Duration firstWait) {
        this.entityManagerFactory = entityManagerFactory;
        this.maxAttempts = maxAttempts;
        this.firstWait = firstWait;
    }

    public <T> T run(Function<EntityManager, T> work) {
        long waitMillis = firstWait.toMillis();
        for (int attempt = 1;; attempt++) {
            try {
                return entityManagerFactory.callInTransaction(work);
            }
            catch (RuntimeException failure) {
                if (attempt >= maxAttempts || !isRetryable(failure)) {
                    throw failure;
                }
                retries.incrementAndGet();
                sleep(waitMillis + ThreadLocalRandom.current().nextLong(waitMillis + 1));
                waitMillis *= 2;
            }
        }
    }

    public long retries() {
        return retries.get();
    }

    public static boolean isRetryable(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && RETRY_SQL_STATE.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", interrupted);
        }
    }

}
