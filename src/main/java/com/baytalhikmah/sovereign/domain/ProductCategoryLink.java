package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class ProductCategoryLink {

    @EmbeddedId
    private ProductCategoryLinkId id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    private boolean primaryCategory;

    @PositiveOrZero
    private int displayOrder;

    protected ProductCategoryLink() {
    }

    ProductCategoryLink(Product product, ProductCategory category, boolean primaryCategory, int displayOrder) {
        this.id = new ProductCategoryLinkId(product.getId(), category.getId());
        this.product = product;
        this.category = category;
        this.primaryCategory = primaryCategory;
        this.displayOrder = displayOrder;
    }

    public ProductCategoryLinkId getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public boolean isPrimaryCategory() {
        return primaryCategory;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

}
