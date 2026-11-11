package com.baytalhikmah.sovereign.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

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

}
