package com.baytalhikmah.sovereign.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind", length = 10)
public abstract class Promotion {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9]{4,20}")
    @Column(unique = true, length = 20)
    private String code;

    protected Promotion() {
    }

    protected Promotion(String code) {
        this.code = code;
    }

    public abstract Money discountFor(Money orderTotal);

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

}
