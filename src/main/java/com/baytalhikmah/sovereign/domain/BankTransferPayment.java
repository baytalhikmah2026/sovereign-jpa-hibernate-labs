package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
public class BankTransferPayment extends Payment {

    @Pattern(regexp = "[A-Z]{2}\\d{2}[A-Z0-9]{10,30}")
    @Column(nullable = false, length = 34)
    private String iban;

    @NotBlank
    @Column(nullable = false, length = 35)
    private String transferReference;

    protected BankTransferPayment() {
    }

    public BankTransferPayment(PurchaseOrder order, Money amount, String iban, String transferReference) {
        super(order, amount);
        this.iban = iban;
        this.transferReference = transferReference;
    }

    @Override
    public String describe() {
        return "Bank transfer " + transferReference + ": " + getAmount();
    }

}
