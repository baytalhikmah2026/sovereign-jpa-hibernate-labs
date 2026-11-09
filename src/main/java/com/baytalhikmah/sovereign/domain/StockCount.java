package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class StockCount {

    @EmbeddedId
    private StockCountId id;

    @PositiveOrZero
    private int countedQuantity;

    protected StockCount() {
    }

    public StockCount(StockCountId id, int countedQuantity) {
        this.id = id;
        this.countedQuantity = countedQuantity;
    }

    public StockCountId getId() {
        return id;
    }

    public int getCountedQuantity() {
        return countedQuantity;
    }

}
