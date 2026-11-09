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
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Payment {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private PurchaseOrder order;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 12, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3, nullable = false))
    private Money amount;

    protected Payment() {
    }

    protected Payment(PurchaseOrder order, Money amount) {
        this.order = order;
        this.amount = amount;
    }

    public abstract String describe();

    public UUID getId() {
        return id;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public Money getAmount() {
        return amount;
    }

}
