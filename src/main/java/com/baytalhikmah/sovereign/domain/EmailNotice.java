package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(indexes = @Index(name = "sov_email_notice_by_customer", columnList = "customer_id"))
public class EmailNotice extends Notice {

    @NotBlank
    @Email
    @Column(nullable = false)
    private String emailAddress;

    protected EmailNotice() {
    }

    public EmailNotice(Customer customer, String subject, String emailAddress) {
        super(customer, subject);
        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

}
