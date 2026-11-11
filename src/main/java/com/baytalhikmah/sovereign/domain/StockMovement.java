package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One line in the warehouse stock ledger: append-only, written in large volumes, numbered by a sequence. */
@Entity
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stockMovementIds")
    @SequenceGenerator(name = "stockMovementIds", sequenceName = "stock_movement_seq", allocationSize = 50)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String sku;

    private int quantityChange;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String reason;

    protected StockMovement() {
    }

    public StockMovement(String sku, int quantityChange, String reason) {
        this.sku = sku;
        this.quantityChange = quantityChange;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public String getReason() {
        return reason;
    }

}
