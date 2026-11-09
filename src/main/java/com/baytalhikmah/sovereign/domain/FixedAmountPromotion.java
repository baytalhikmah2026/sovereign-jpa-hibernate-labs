package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("FIXED")
public class FixedAmountPromotion extends Promotion {

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "fixed_off_amount", precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "fixed_off_currency", length = 3))
    private Money fixedOff;

    protected FixedAmountPromotion() {
    }

    public FixedAmountPromotion(String code, Money fixedOff) {
        super(code);
        this.fixedOff = fixedOff;
    }

    @Override
    public Money discountFor(Money orderTotal) {
        return new Money(orderTotal.amount().min(fixedOff.amount()), orderTotal.currency());
    }

}
