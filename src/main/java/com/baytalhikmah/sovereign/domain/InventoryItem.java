package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@IdClass(InventoryItemId.class)
public class InventoryItem {

    @Id
    @Column(length = 10)
    private String warehouseCode;

    @Id
    @Column(length = 40)
    private String sku;

    @PositiveOrZero
    private int quantityOnHand;

    protected InventoryItem() {
    }

    public InventoryItem(String warehouseCode, String sku, int quantityOnHand) {
        this.warehouseCode = warehouseCode;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void receive(int quantity) {
        quantityOnHand += quantity;
    }

    public void ship(int quantity) {
        if (quantity > quantityOnHand) {
            throw new IllegalStateException("Only " + quantityOnHand + " left of " + sku + " in " + warehouseCode);
        }
        quantityOnHand -= quantity;
    }

}
