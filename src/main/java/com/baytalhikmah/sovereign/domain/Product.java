package com.baytalhikmah.sovereign.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

import com.baytalhikmah.sovereign.persistence.EmbeddingConverter;
import com.baytalhikmah.sovereign.persistence.ProductSpecsConverter;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Product {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Size(max = 40)
    @Column(unique = true)
    private String sku;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 12, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3, nullable = false))
    private Money price;

    @Convert(converter = ProductSpecsConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private ProductSpecs specs;

    @Convert(converter = EmbeddingConverter.class)
    @ColumnTransformer(write = "?::vector")
    @Column(columnDefinition = "vector(3)")
    private float[] embedding;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategoryLink> categoryLinks = new ArrayList<>();

    protected Product() {
    }

    public Product(String sku, String name, Money price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public void changePrice(Money newPrice) {
        this.price = newPrice;
    }

    public ProductSpecs getSpecs() {
        return specs;
    }

    public float[] getEmbedding() {
        return embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
    }

    public void updateCatalogueData(ProductSpecs newSpecs, float[] newEmbedding) {
        this.specs = newSpecs;
        this.embedding = newEmbedding == null ? null : Arrays.copyOf(newEmbedding, newEmbedding.length);
    }

    public ProductCategoryLink addToCategory(ProductCategory category, boolean primary, int displayOrder) {
        ProductCategoryLink link = new ProductCategoryLink(this, category, primary, displayOrder);
        categoryLinks.add(link);
        return link;
    }

    public void removeFromCategory(ProductCategory category) {
        categoryLinks.removeIf(link -> link.getCategory().getId().equals(category.getId()));
    }

    public List<ProductCategoryLink> getCategoryLinks() {
        return Collections.unmodifiableList(categoryLinks);
    }

}
