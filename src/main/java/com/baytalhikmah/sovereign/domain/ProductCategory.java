package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(indexes = @Index(name = "sov_product_category_by_parent", columnList = "parent_id"))
public class ProductCategory {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Pattern(regexp = "[a-z0-9-]{2,40}")
    @Column(unique = true, length = 40)
    private String slug;

    @NotBlank
    @Size(max = 80)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProductCategory parent;

    protected ProductCategory() {
    }

    public ProductCategory(String slug, String name) {
        this.slug = slug;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public ProductCategory getParent() {
        return parent;
    }

    public void placeUnder(ProductCategory newParent) {
        this.parent = newParent;
    }

}
