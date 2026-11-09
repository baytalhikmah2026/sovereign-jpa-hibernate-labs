package com.baytalhikmah.sovereign.bench;

import jakarta.persistence.Entity;

@Entity
public class BenchSmsNotice extends BenchNotice {

    private String phoneNumber;

    protected BenchSmsNotice() {
    }

    public BenchSmsNotice(String subject, String phoneNumber) {
        super(subject);
        this.phoneNumber = phoneNumber;
    }

}
