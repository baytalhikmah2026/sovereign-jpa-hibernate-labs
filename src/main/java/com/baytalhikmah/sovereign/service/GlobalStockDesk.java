package com.baytalhikmah.sovereign.service;

import java.util.List;

import com.baytalhikmah.sovereign.domain.RegionalStock;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/** Reserves regional stock in the distributed cluster; conflicting reservations are retried, never lost. */
public class GlobalStockDesk {

    private final TransactionRetry transactions;

    public GlobalStockDesk(TransactionRetry transactions) {
        this.transactions = transactions;
    }

    public RegionalStock register(String sku, String region, String countryCode, int quantity) {
        return transactions.run(entityManager -> {
            RegionalStock stock = new RegionalStock(sku, region, countryCode, quantity);
            entityManager.persist(stock);
            return stock;
        });
    }

    /** Takes units out of the region's stock and returns what is left. */
    public int reserve(String sku, String region, int units, Runnable afterRead) {
        return transactions.run(entityManager -> {
            RegionalStock stock = entityManager.createQuery(
                    "select s from RegionalStock s where s.sku = :sku and s.region = :region", RegionalStock.class)
                .setParameter("sku", sku)
                .setParameter("region", region)
                .getSingleResult();
            afterRead.run();
            stock.reserve(units);
            return stock.getQuantity();
        });
    }

    public int reserve(String sku, String region, int units) {
        return reserve(sku, region, units, () -> {
        });
    }

    public int quantity(String sku, String region) {
        return transactions.run(entityManager -> entityManager.createQuery(
                "select s.quantity from RegionalStock s where s.sku = :sku and s.region = :region", Integer.class)
            .setParameter("sku", sku)
            .setParameter("region", region)
            .getSingleResult());
    }

    /**
     * Follower read: answered by the nearest copy of the data as it was a few seconds ago, without asking the
     * region that holds the lease. Fast everywhere; use it where slightly old numbers are fine (shop pages, reports).
     */
    public int quantityNearby(String sku, String region) {
        return transactions.run(entityManager -> {
            entityManager.createNativeQuery("set transaction as of system time follower_read_timestamp()")
                .executeUpdate();
            return entityManager.createQuery(
                    "select s.quantity from RegionalStock s where s.sku = :sku and s.region = :region", Integer.class)
                .setParameter("sku", sku)
                .setParameter("region", region)
                .getSingleResult();
        });
    }

    /**
     * The checkout path: the stock row is locked when it is read (SELECT ... FOR UPDATE), so concurrent buyers of the
     * same product queue for the row instead of colliding and retrying.
     */
    public int reserveForCheckout(String sku, String region, int units) {
        return transactions.run(entityManager -> {
            RegionalStock stock = find(entityManager, sku, region, LockModeType.PESSIMISTIC_WRITE);
            stock.reserve(units);
            return stock.getQuantity();
        });
    }

    /** Compensation: gives reserved units back when the rest of the checkout failed. */
    public int release(String sku, String region, int units) {
        return transactions.run(entityManager -> {
            RegionalStock stock = find(entityManager, sku, region, LockModeType.PESSIMISTIC_WRITE);
            stock.release(units);
            return stock.getQuantity();
        });
    }

    /** Registers the stock row only if it does not exist yet; returns the current quantity. */
    public int ensureStock(String sku, String region, String countryCode, int quantity) {
        return transactions.run(entityManager -> {
            List<RegionalStock> existing = entityManager.createQuery(
                    "select s from RegionalStock s where s.sku = :sku and s.region = :region", RegionalStock.class)
                .setParameter("sku", sku)
                .setParameter("region", region)
                .getResultList();
            if (!existing.isEmpty()) {
                return existing.getFirst().getQuantity();
            }
            entityManager.persist(new RegionalStock(sku, region, countryCode, quantity));
            return quantity;
        });
    }

    private static RegionalStock find(EntityManager entityManager, String sku, String region, LockModeType lock) {
        return entityManager.createQuery(
                "select s from RegionalStock s where s.sku = :sku and s.region = :region", RegionalStock.class)
            .setParameter("sku", sku)
            .setParameter("region", region)
            .setLockMode(lock)
            .getSingleResult();
    }

}
