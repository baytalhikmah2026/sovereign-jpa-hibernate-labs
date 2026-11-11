package com.baytalhikmah.sovereign.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "reference")
public class TaxRate {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @Column(nullable = false)
    private LocalDate validFrom;

    protected TaxRate() {
    }

    public TaxRate(String code, String countryCode, BigDecimal ratePercent, LocalDate validFrom) {
        this.code = code;
        this.countryCode = countryCode;
        this.ratePercent = ratePercent;
        this.validFrom = validFrom;
    }

    public Money taxOn(Money net) {
        return new Money(net.amount().multiply(ratePercent).movePointLeft(2), net.currency());
    }

    public String getCode() {
        return code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

}
