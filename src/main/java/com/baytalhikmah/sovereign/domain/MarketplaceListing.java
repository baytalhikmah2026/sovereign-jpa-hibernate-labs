package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A product offered by one shop on the shared marketplace. All shops share one table; tenant_id says whose row it is. */
@Entity
@Table(indexes = @Index(name = "sov_marketplace_listing_by_tenant", columnList = "tenant_id"))
@FilterDef(name = MarketplaceListing.TENANT_FILTER, parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = MarketplaceListing.TENANT_FILTER, condition = "tenant_id = :tenantId")
public class MarketplaceListing {

    public static final String TENANT_FILTER = "tenant";

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Size(max = 21)
    @Column(nullable = false, updatable = false, length = 21)
    private String tenantId;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 12, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3, nullable = false))
    private Money price;

    protected MarketplaceListing() {
    }

    public MarketplaceListing(String tenantId, String title, Money price) {
        this.tenantId = tenantId;
        this.title = title;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public Money getPrice() {
        return price;
    }

}
