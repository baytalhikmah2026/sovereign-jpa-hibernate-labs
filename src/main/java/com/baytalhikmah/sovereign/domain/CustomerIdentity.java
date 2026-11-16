package com.baytalhikmah.sovereign.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.baytalhikmah.sovereign.persistence.EncryptedStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** An identity document checked for a customer ("know your customer"). Number and birth date are stored encrypted. */
@Entity
@Table(indexes = @Index(name = "sov_customer_identity_by_customer", columnList = "customer_id"))
public class CustomerIdentity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotBlank
    @Pattern(regexp = "PASSPORT|ID_CARD|DRIVING_LICENCE")
    @Column(nullable = false, length = 20)
    private String documentType;

    @NotBlank
    @Size(max = 40)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 200)
    private String documentNumber;

    @NotBlank
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 200)
    private String dateOfBirth;

    protected CustomerIdentity() {
    }

    public CustomerIdentity(Customer customer, String documentType, String documentNumber, LocalDate dateOfBirth) {
        this.customer = customer;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.dateOfBirth = dateOfBirth.toString();
    }

    public UUID getId() {
        return id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDate getDateOfBirth() {
        return LocalDate.parse(dateOfBirth);
    }

}
