package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Positive;

@Entity
public class OrderLine {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Product product;

    @Positive
    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount", precision = 12, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", length = 3, nullable = false))
    private Money unitPrice;

    protected OrderLine() {
    }

    OrderLine(PurchaseOrder order, Product product, int quantity, Money unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    void detachFromOrder() {
        this.order = null;
    }

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }

    public UUID getId() {
        return id;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

}
