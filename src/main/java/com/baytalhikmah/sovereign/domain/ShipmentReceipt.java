package com.baytalhikmah.sovereign.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Proof that one shipment was carried out. Its key comes from the caller, so a repeated attempt of the
 * same shipment finds the receipt and knows the work is already done.
 */
@Entity
public class ShipmentReceipt {

    @Id
    @Column(length = 40)
    private String shipmentKey;

    @Column(nullable = false, length = 10)
    private String warehouseCode;

    @Column(nullable = false, length = 40)
    private String sku;

    private int quantity;

    @Column(nullable = false)
    private Instant shippedAt;

    protected ShipmentReceipt() {
    }

    public ShipmentReceipt(String shipmentKey, String warehouseCode, String sku, int quantity) {
        this.shipmentKey = shipmentKey;
        this.warehouseCode = warehouseCode;
        this.sku = sku;
        this.quantity = quantity;
        this.shippedAt = Instant.now();
    }

    public String getShipmentKey() {
        return shipmentKey;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

}
