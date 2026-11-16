package com.baytalhikmah.sovereign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One entry of the GDPR record of processing activities. The table lives in the Flyway-owned schema compliance. */
@Entity
@Table(schema = "compliance", name = "processing_activity")
public class ProcessingActivity {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(nullable = false, length = 30)
    private String legalBasis;

    @Column(nullable = false, length = 300)
    private String dataCategories;

    @Column(nullable = false)
    private int retentionDays;

    @Column(nullable = false, length = 120)
    private String controllerContact;

    protected ProcessingActivity() {
    }

    public ProcessingActivity(String code, String purpose, String legalBasis, String dataCategories,
            int retentionDays, String controllerContact) {
        this.code = code;
        this.purpose = purpose;
        this.legalBasis = legalBasis;
        this.dataCategories = dataCategories;
        this.retentionDays = retentionDays;
        this.controllerContact = controllerContact;
    }

    public String getCode() {
        return code;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public String getDataCategories() {
        return dataCategories;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getControllerContact() {
        return controllerContact;
    }

}
