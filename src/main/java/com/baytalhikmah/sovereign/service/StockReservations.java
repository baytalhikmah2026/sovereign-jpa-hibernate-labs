package com.baytalhikmah.sovereign.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.StockMovement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Reads stock balances from the ledger (Lab 54) and ships against them, at a chosen isolation level. */
@Repository
public class StockReservations {

    /** The same balance read twice in one transaction. */
    public record TwoReadings(long first, long second) {
    }

    @PersistenceContext
    private EntityManager entityManager;

    private final PlatformTransactionManager transactionManager;

    public StockReservations(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /** The balance of a SKU: the sum of all its ledger movements. */
    public long balance(String sku) {
        return entityManager.createQuery(
                "select coalesce(sum(m.quantityChange), 0L) from StockMovement m where m.sku = :sku", Long.class)
            .setParameter("sku", sku)
            .getSingleResult();
    }

    /** Reads the balance, lets {@code betweenReadings} run, and reads it again, all in one transaction. */
    public TwoReadings balanceTwice(String sku, int isolationLevel, Runnable betweenReadings) {
        return inTransaction(isolationLevel, () -> {
            long first = balance(sku);
            betweenReadings.run();
            long second = balance(sku);
            return new TwoReadings(first, second);
        });
    }

    /** Ships only if the balance covers the quantity. {@code afterCheck} runs between the check and the write. */
    public boolean shipIfAvailable(String sku, int quantity, int isolationLevel, Runnable afterCheck) {
        return inTransaction(isolationLevel, () -> {
            long available = balance(sku);
            afterCheck.run();
            if (available < quantity) {
                return false;
            }
            entityManager.persist(new StockMovement(sku, -quantity, "shipment"));
            return true;
        });
    }

    private <T> T inTransaction(int isolationLevel, Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(isolationLevel);
        return transaction.execute(status -> work.get());
    }

}
