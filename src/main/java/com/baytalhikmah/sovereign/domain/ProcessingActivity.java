package com.baytalhikmah.sovereign.domain;

import org.hibernate.annotations.Formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One entry of the GDPR record of processing activities. The table lives in the Flyway-owned schema compliance.
 * Release 2 of the rename controller_contact -> dpo_email: this code writes only dpo_email, never the old column.
 */
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

    /** The new column. The only contact column this release writes. */
    @Column(name = "dpo_email", length = 120)
    private String dpoEmail;

    /** The old column, read-only: a trigger keeps it filled for older application copies until release 3 drops it. */
    @Column(name = "controller_contact", insertable = false, updatable = false)
    private String legacyContact;

    /** What the code reads: the new column, or the old one for rows written while the trigger was not firing. */
    @Formula("coalesce(dpo_email, controller_contact)")
    private String privacyContact;

    protected ProcessingActivity() {
    }

    public ProcessingActivity(String code, String purpose, String legalBasis, String dataCategories,
            int retentionDays, String dpoEmail) {
        this.code = code;
        this.purpose = purpose;
        this.legalBasis = legalBasis;
        this.dataCategories = dataCategories;
        this.retentionDays = retentionDays;
        this.dpoEmail = dpoEmail;
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

    /** Filled when the entity is loaded from the database (a formula is calculated by the SELECT). */
    public String getPrivacyContact() {
        return privacyContact;
    }

    public String getLegacyContact() {
        return legacyContact;
    }

    public void changeDpoEmail(String newDpoEmail) {
        this.dpoEmail = newDpoEmail;
    }

}
