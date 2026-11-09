package com.baytalhikmah.sovereign.bench;

import jakarta.persistence.Entity;

@Entity
public class BenchEmailNotice extends BenchNotice {

    private String emailAddress;

    protected BenchEmailNotice() {
    }

    public BenchEmailNotice(String subject, String emailAddress) {
        super(subject);
        this.emailAddress = emailAddress;
    }

}
