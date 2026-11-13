package com.baytalhikmah.sovereign.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.baytalhikmah.sovereign.persistence.SqlStates;

/**
 * Ships through ShipmentDesk and survives collisions and short database outages: each failed attempt that
 * may succeed later is retried in a new transaction, after a pause that doubles every time (with jitter).
 */
@Service
public class ResilientShipper {

    /** How one shipment ended: shipped by the last attempt, or found already shipped, and after how many attempts. */
    public record Outcome(String shipmentKey, boolean shippedNow, int attempts) {
    }

    static final int MAX_ATTEMPTS = 20;

    static final Duration FIRST_PAUSE = Duration.ofMillis(20);

    static final Duration LONGEST_PAUSE = Duration.ofSeconds(1);

    private final ShipmentDesk shipmentDesk;

    public ResilientShipper(ShipmentDesk shipmentDesk) {
        this.shipmentDesk = shipmentDesk;
    }

    public Outcome ship(String shipmentKey, String warehouseCode, String sku, int quantity) {
        for (int attempt = 1; ; attempt++) {
            try {
                return new Outcome(shipmentKey, shipmentDesk.shipOnce(shipmentKey, warehouseCode, sku, quantity),
                    attempt);
            }
            catch (RuntimeException failure) {
                if (!isWorthRetrying(failure) || attempt == MAX_ATTEMPTS) {
                    throw failure;
                }
                Duration pause = backoff(attempt);
                System.out.println(shipmentKey + ": attempt " + attempt + " failed ("
                    + failure.getClass().getSimpleName() + ", SQLSTATE " + SqlStates.of(failure) + "), retry in "
                    + pause.toMillis() + " ms");
                sleep(pause);
            }
        }
    }

    /**
     * Collisions, lock waits and lost connections can succeed on a later attempt; business errors cannot.
     * A duplicate receipt key (23505) means an earlier attempt of this shipment has committed after all:
     * the next attempt finds its receipt and reports the shipment as already done.
     */
    static boolean isWorthRetrying(Throwable failure) {
        if (failure instanceof OptimisticLockingFailureException
                || failure instanceof PessimisticLockingFailureException
                || failure instanceof TransientDataAccessException
                || failure instanceof DataAccessResourceFailureException
                || failure instanceof CannotCreateTransactionException) {
            return true;
        }
        String sqlState = SqlStates.of(failure);
        return sqlState.startsWith("08") || sqlState.equals("40001") || sqlState.equals("40P01")
            || sqlState.equals("55P03") || sqlState.equals("23505");
    }

    /** 20, 40, 80, ... ms, at most 1 s, plus a random extra of up to the same amount ("jitter"). */
    static Duration backoff(int attempt) {
        long base = Math.min(LONGEST_PAUSE.toMillis(), FIRST_PAUSE.toMillis() << Math.min(attempt - 1, 10));
        return Duration.ofMillis(base + ThreadLocalRandom.current().nextLong(base + 1));
    }

    private static void sleep(Duration pause) {
        try {
            Thread.sleep(pause);
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", interrupted);
        }
    }

}
