package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
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

}
