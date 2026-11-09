package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
public class CardPayment extends Payment {

    @NotBlank
    @Column(nullable = false, length = 20)
    private String cardBrand;

    @Pattern(regexp = "\\d{4}")
    @Column(nullable = false, length = 4)
    private String lastFourDigits;

    protected CardPayment() {
    }

    public CardPayment(PurchaseOrder order, Money amount, String cardBrand, String lastFourDigits) {
        super(order, amount);
        this.cardBrand = cardBrand;
        this.lastFourDigits = lastFourDigits;
    }

    @Override
    public String describe() {
        return cardBrand + " card ending " + lastFourDigits + ": " + getAmount();
    }

}
