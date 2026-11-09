package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Product {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Size(max = 40)
    @Column(unique = true)
    private String sku;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 12, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3, nullable = false))
    private Money price;

    protected Product() {
    }

    public Product(String sku, String name, Money price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public void changePrice(Money newPrice) {
        this.price = newPrice;
    }

}
