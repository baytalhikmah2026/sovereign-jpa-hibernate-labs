package com.baytalhikmah.sovereign.domain;

import java.math.BigDecimal;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@DiscriminatorValue("PERCENT")
public class PercentagePromotion extends Promotion {

    @Min(1)
    @Max(90)
    private Integer percentOff;

    protected PercentagePromotion() {
    }

    public PercentagePromotion(String code, int percentOff) {
        super(code);
        this.percentOff = percentOff;
    }

    @Override
    public Money discountFor(Money orderTotal) {
        BigDecimal discount = orderTotal.amount().multiply(BigDecimal.valueOf(percentOff)).movePointLeft(2);
        return new Money(discount, orderTotal.currency());
    }

}
