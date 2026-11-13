package com.baytalhikmah.sovereign.domain;

import com.baytalhikmah.sovereign.persistence.StockChangeListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Version;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@EntityListeners(StockChangeListener.class)
@IdClass(InventoryItemId.class)
@NamedStoredProcedureQuery(name = "InventoryItem.restock", procedureName = "sov_restock", parameters = {
        @StoredProcedureParameter(name = "p_warehouse_code", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "p_sku", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "p_quantity", mode = ParameterMode.IN, type = Integer.class) })
public class InventoryItem {

    @Id
    @Column(length = 10)
    private String warehouseCode;

    @Id
    @Column(length = 40)
    private String sku;

    @PositiveOrZero
    private int quantityOnHand;

    @Version
    private long version;

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

    public long getVersion() {
        return version;
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
