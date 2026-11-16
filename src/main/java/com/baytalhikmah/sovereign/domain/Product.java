package com.baytalhikmah.sovereign.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.type.SqlTypes;

import com.baytalhikmah.sovereign.persistence.DepartmentRoutingBinder;
import com.baytalhikmah.sovereign.persistence.ProductSpecsConverter;
import com.baytalhikmah.sovereign.query.ProductSales;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Audited(withModifiedFlag = true)
@Indexed(index = "products", routingBinder = @RoutingBinderRef(type = DepartmentRoutingBinder.class))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "catalogue")
@Table(indexes = @Index(name = "sov_product_by_name_id", columnList = "name, id"))
@SqlResultSetMapping(name = "ProductSales", classes = @ConstructorResult(
        targetClass = ProductSales.class,
        columns = {
            @ColumnResult(name = "sku"),
            @ColumnResult(name = "name"),
            @ColumnResult(name = "units", type = Long.class),
            @ColumnResult(name = "revenue", type = BigDecimal.class),
            @ColumnResult(name = "orders") }))
public class Product {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Size(max = 40)
    @Column(unique = true)
    @KeywordField(sortable = Sortable.YES)
    private String sku;

    @NotBlank
    @Size(max = 120)
    @FullTextField
    private String name;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 12, scale = 2, nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3, nullable = false))
    private Money price;

    @NotAudited
    @Convert(converter = ProductSpecsConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private ProductSpecs specs;

    @NotAudited
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3)
    private float[] embedding;

    @ElementCollection
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "catalogue_images")
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"),
            indexes = @Index(name = "sov_product_image_by_product", columnList = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_url", nullable = false, length = 300)
    private List<String> imageUrls = new ArrayList<>();

    @NotAudited
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategoryLink> categoryLinks = new ArrayList<>();

    @NotAudited
    @OneToMany(mappedBy = "product")
    @BatchSize(size = 25)
    private List<ProductReview> reviews = new ArrayList<>();

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

    /** The department code: the SKU part before the first dash ("KIT" for KIT-Q76-KETTLE). Not a column. */
    @KeywordField
    @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "sku")))
    public String getDepartment() {
        int dash = sku.indexOf('-');
        return dash < 0 ? sku : sku.substring(0, dash);
    }

    public Money getPrice() {
        return price;
    }

    public void rename(String newName) {
        this.name = newName;
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

    public void addImage(String imageUrl) {
        imageUrls.add(imageUrl);
    }

    public void removeImage(String imageUrl) {
        imageUrls.remove(imageUrl);
    }

    public List<String> getImageUrls() {
        return Collections.unmodifiableList(imageUrls);
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

    public List<ProductReview> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

}
