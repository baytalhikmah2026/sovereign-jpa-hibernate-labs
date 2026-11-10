package com.baytalhikmah.sovereign.domain;

import java.sql.Blob;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** A downloadable product manual (PDF, video, firmware). The file itself lives in a PostgreSQL large object. */
@Entity
@Table(indexes = @Index(name = "sov_product_manual_by_product", columnList = "product_id"))
public class ProductManual {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 120)
    @Column(length = 120)
    private String fileName;

    @PositiveOrZero
    private long sizeBytes;

    @Lob
    @Column(nullable = false)
    private Blob content;

    protected ProductManual() {
    }

    public ProductManual(Product product, String fileName, long sizeBytes, Blob content) {
        this.product = product;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Blob getContent() {
        return content;
    }

}
