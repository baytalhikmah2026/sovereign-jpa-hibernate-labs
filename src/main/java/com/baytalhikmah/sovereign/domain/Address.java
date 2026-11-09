package com.baytalhikmah.sovereign.domain;

import java.util.Locale;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Embeddable
public record Address(
        @NotBlank @Size(max = 120) String street,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode) {

    public Address {
        countryCode = countryCode == null ? null : countryCode.toUpperCase(Locale.ROOT);
    }

    public Address withStreet(String newStreet) {
        return new Address(newStreet, city, postalCode, countryCode);
    }

}
