package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Stock of one product in one region of the shop chain. Kept in the distributed SQL cluster, so every region sees
 * one consistent number. Random (version 4) UUIDs spread new rows over the whole cluster instead of one hot spot.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "sov_regional_stock_sku_region", columnNames = { "sku", "region" }))
public class RegionalStock {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 20)
    private String region;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private int quantity;

    protected RegionalStock() {
    }

    public RegionalStock(String sku, String region, String countryCode, int quantity) {
        this.sku = sku;
        this.region = region;
        this.countryCode = countryCode;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getRegion() {
        return region;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reserve(int units) {
        if (units > quantity) {
            throw new IllegalStateException("Only " + quantity + " units of " + sku + " left in " + region);
        }
        quantity -= units;
    }

}
