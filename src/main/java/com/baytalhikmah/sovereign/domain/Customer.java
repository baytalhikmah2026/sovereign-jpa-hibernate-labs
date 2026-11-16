package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@SecondaryTable(name = "customer_loyalty", pkJoinColumns = @PrimaryKeyJoinColumn(name = "customer_id"))
public class Customer {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotNull
    @SignupChannel
    private String signupChannel;

    @Valid
    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "billing_street"))
    @AttributeOverride(name = "city", column = @Column(name = "billing_city"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code"))
    @AttributeOverride(name = "countryCode", column = @Column(name = "billing_country_code"))
    private Address billingAddress;

    @Valid
    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "shipping_street"))
    @AttributeOverride(name = "city", column = @Column(name = "shipping_city"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code"))
    @AttributeOverride(name = "countryCode", column = @Column(name = "shipping_country_code"))
    private Address shippingAddress;

    @Pattern(regexp = "BRONZE|SILVER|GOLD")
    @Column(table = "customer_loyalty", length = 10)
    private String loyaltyTier;

    @PositiveOrZero
    @Column(table = "customer_loyalty")
    private Integer loyaltyPoints;

    protected Customer() {
    }

    public Customer(String fullName, String email) {
        this(fullName, email, "web");
    }

    public Customer(String fullName, String email, String signupChannel) {
        this.fullName = fullName;
        this.email = email;
        this.signupChannel = signupChannel;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getSignupChannel() {
        return signupChannel;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public String getLoyaltyTier() {
        return loyaltyTier;
    }

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void joinLoyalty(String tier) {
        this.loyaltyTier = tier;
        this.loyaltyPoints = 0;
    }

    public void addLoyaltyPoints(int points) {
        if (loyaltyTier == null) {
            throw new IllegalStateException(fullName + " is not a loyalty member");
        }
        loyaltyPoints += points;
    }

    public void moveTo(Address billing, Address shipping) {
        this.billingAddress = billing;
        this.shippingAddress = shipping;
    }

    /** GDPR erasure: replaces every personal field; the row stays so that kept records (orders) still point to it. */
    public void anonymise(String placeholderName, String placeholderEmail) {
        this.fullName = placeholderName;
        this.email = placeholderEmail;
        this.billingAddress = null;
        this.shippingAddress = null;
        this.loyaltyTier = null;
        this.loyaltyPoints = null;
    }

}
