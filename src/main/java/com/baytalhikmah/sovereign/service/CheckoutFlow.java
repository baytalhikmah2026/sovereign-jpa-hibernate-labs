package com.baytalhikmah.sovereign.service;

import java.util.UUID;

/**
 * One checkout across the data grid: reserve the unit in the global stock (CockroachDB), then place the order with
 * its outbox event (PostgreSQL). Two databases cannot share one transaction, so this is a small saga: if placing
 * the order fails, the reservation is given back (compensation).
 */
public class CheckoutFlow {

    private final GlobalStockDesk stock;

    private final OrderDesk orders;

    private final String region;

    public CheckoutFlow(GlobalStockDesk stock, OrderDesk orders, String region) {
        this.stock = stock;
        this.orders = orders;
        this.region = region;
    }

    public record Receipt(UUID orderId, int unitsLeftInRegion) {
    }

    public Receipt checkout(String orderNumber, String customerEmail, String sku, int quantity) {
        int left = stock.reserveForCheckout(sku, region, quantity);
        try {
            return new Receipt(orders.placeOrder(orderNumber, customerEmail, sku, quantity), left);
        }
        catch (RuntimeException failure) {
            stock.release(sku, region, quantity);
            throw failure;
        }
    }

}
