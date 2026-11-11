package com.baytalhikmah.sovereign.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UuidGenerator;

import com.baytalhikmah.sovereign.audit.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(indexes = @Index(name = "sov_product_category_by_parent", columnList = "parent_id"))
public class ProductCategory implements Audited {

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

    @OneToMany(mappedBy = "parent")
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("slug")
    private List<ProductCategory> children = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    @Column(length = 60)
    private String changedBy;

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

    public List<ProductCategory> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void placeUnder(ProductCategory newParent) {
        this.parent = newParent;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String getChangedBy() {
        return changedBy;
    }

}
