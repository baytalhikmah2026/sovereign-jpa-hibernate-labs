package com.baytalhikmah.sovereign.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Money(
        @Column(precision = 12, scale = 2) BigDecimal amount,
        @Column(length = 3) String currency) {

    public Money {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("Money needs an amount and a currency");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + amount);
        }
        Currency.getInstance(currency);
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add " + other.currency + " to " + currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }

}
