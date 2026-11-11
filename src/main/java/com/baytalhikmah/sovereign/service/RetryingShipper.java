package com.baytalhikmah.sovereign.service;

import java.time.Duration;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.StockDesk.StockLevel;

/** Ships stock and tries again, in a fresh transaction, when another user changed the item first. */
@Service
public class RetryingShipper {

    /** The stock after a successful shipment and how many attempts it took. */
    public record Shipment(StockLevel stock, int attempts) {
    }

    private final StockDesk stockDesk;

    public RetryingShipper(StockDesk stockDesk) {
        this.stockDesk = stockDesk;
    }

    /** Retries on optimistic locking conflicts; {@code firstAttemptThinkingTime} runs only in attempt 1. */
    public Shipment shipWithRetry(String warehouseCode, String sku, int quantity, int maxAttempts,
            Runnable firstAttemptThinkingTime) {
        for (int attempt = 1; ; attempt++) {
            try {
                Runnable thinkingTime = attempt == 1 ? firstAttemptThinkingTime : () -> { };
                return new Shipment(stockDesk.shipOnce(warehouseCode, sku, quantity, thinkingTime), attempt);
            }
            catch (OptimisticLockingFailureException conflict) {
                if (attempt == maxAttempts) {
                    throw conflict;
                }
                System.out.println("Attempt " + attempt + " lost the race for " + sku + ", trying again");
            }
        }
    }

    /** Takes a write lock and ships; when the lock wait times out, pauses and tries again in a new transaction. */
    public Shipment shipWhenFree(String warehouseCode, String sku, int quantity, int maxAttempts, Duration pause) {
        for (int attempt = 1; ; attempt++) {
            try {
                return new Shipment(stockDesk.shipLocked(warehouseCode, sku, quantity, () -> { }), attempt);
            }
            catch (PessimisticLockingFailureException busy) {
                if (attempt == maxAttempts) {
                    throw busy;
                }
                System.out.println("Attempt " + attempt + ": " + sku + " is locked (SQLSTATE "
                    + SqlStates.of(busy) + "), waiting " + pause.toMillis() + " ms");
                sleep(pause);
            }
        }
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
