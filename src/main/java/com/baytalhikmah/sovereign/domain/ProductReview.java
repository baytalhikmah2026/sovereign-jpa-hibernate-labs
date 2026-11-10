package com.baytalhikmah.sovereign.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Entity
@Table(indexes = {
        @Index(name = "sov_product_review_by_product", columnList = "product_id"),
        @Index(name = "sov_product_review_by_customer", columnList = "customer_id") })
@SQLDelete(sql = "update sov_product_review set deleted_at = current_timestamp where id = ?")
@SQLRestriction("deleted_at is null")
public class ProductReview {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Customer customer;

    @Min(1)
    @Max(5)
    private int rating;

    @Size(max = 2000)
    @Basic(fetch = FetchType.LAZY)
    @Column(length = 2000)
    private String comment;

    @Column(insertable = false, updatable = false)
    private Instant deletedAt;

    protected ProductReview() {
    }

    public ProductReview(Product product, Customer customer, int rating, String comment) {
        this.product = product;
        this.customer = customer;
        this.rating = rating;
        this.comment = comment;
    }

    public UUID getId() {
        return id;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

}
