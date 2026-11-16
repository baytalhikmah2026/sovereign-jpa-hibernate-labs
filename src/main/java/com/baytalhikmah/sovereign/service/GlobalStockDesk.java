package com.baytalhikmah.sovereign.service;

import com.baytalhikmah.sovereign.domain.RegionalStock;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;

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

}
