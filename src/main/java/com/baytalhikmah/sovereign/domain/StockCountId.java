package com.baytalhikmah.sovereign.domain;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record StockCountId(
        @Column(length = 10) String warehouseCode,
        @Column(length = 40) String sku,
        LocalDate countDate) implements Serializable {
}
