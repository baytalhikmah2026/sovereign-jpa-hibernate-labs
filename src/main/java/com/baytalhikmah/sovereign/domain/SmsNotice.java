package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(indexes = @Index(name = "sov_sms_notice_by_customer", columnList = "customer_id"))
public class SmsNotice extends Notice {

    @Pattern(regexp = "\\+[1-9]\\d{7,14}")
    @Column(nullable = false, length = 16)
    private String phoneNumber;

    protected SmsNotice() {
    }

    public SmsNotice(Customer customer, String subject, String phoneNumber) {
        super(customer, subject);
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

}
