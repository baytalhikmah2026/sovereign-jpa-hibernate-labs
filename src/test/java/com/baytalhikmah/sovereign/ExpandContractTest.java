package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.baytalhikmah.sovereign.domain.ProcessingActivity;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ExpandContractTest {

    private static final String OLD_APP_COLUMN =
        "select controller_contact from compliance.sov_processing_activity where code = ?";

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    @Autowired
    PostgreSQLContainer postgres;

    @Test
    void oldAndNewReleasesWorkSideBySideAndTheContractRemovesTheOldColumn() {
        // Release 1 (the previous code) still writes only controller_contact.
        jdbc.update("""
                insert into compliance.sov_processing_activity
                (code, purpose, legal_basis, data_categories, retention_days, controller_contact)
                values ('CCTV_STORE', 'Video surveillance in shops', 'LEGITIMATE_INTEREST', 'images', 30,
                        'cctv@sovereign-shop.test')""");
        String newReadsOldRow = read("CCTV_STORE").getPrivacyContact();

        // Release 2 (this code) writes only dpo_email.
        transaction.executeWithoutResult(status -> entityManager.persist(new ProcessingActivity("LOYALTY_PROGRAMME",
            "Run the loyalty programme", "CONTRACT", "name, e-mail, points", 1095, "loyalty@sovereign-shop.test")));
        String oldReadsNewRow = jdbc.queryForObject(OLD_APP_COLUMN, String.class, "LOYALTY_PROGRAMME");

        // Each release changes a contact the way it knows; the other one sees the change.
        jdbc.update("update compliance.sov_processing_activity set controller_contact = ? where code = ?",
            "rewards@sovereign-shop.test", "LOYALTY_PROGRAMME");
        String newSeesOldUpdate = read("LOYALTY_PROGRAMME").getPrivacyContact();
        transaction.executeWithoutResult(status ->
            entityManager.find(ProcessingActivity.class, "MARKETING_EMAIL").changeDpoEmail("dpo@sovereign-shop.test"));
        String oldSeesNewUpdate = jdbc.queryForObject(OLD_APP_COLUMN, String.class, "MARKETING_EMAIL");

        // A row loaded while triggers are off (as replication and some restore tools do): dpo_email stays empty.
        transaction.executeWithoutResult(status -> {
            jdbc.execute("set local session_replication_role = replica");
            jdbc.update("""
                    insert into compliance.sov_processing_activity
                    (code, purpose, legal_basis, data_categories, retention_days, controller_contact)
                    values ('PAYROLL', 'Pay employees', 'LEGAL_OBLIGATION', 'bank details', 3650,
                            'hr@sovereign-shop.test')""");
        });
        String withoutTrigger = jdbc.queryForObject(
            "select coalesce(dpo_email, '(empty)') from compliance.sov_processing_activity where code = 'PAYROLL'",
            String.class);
        String formulaFallback = read("PAYROLL").getPrivacyContact();

        // Release 3: the contract migration, run on a fresh database with both locations.
        jdbc.execute("create database lab95_contract");
        String url = postgres.getJdbcUrl().replace("/sovereign", "/lab95_contract");
        MigrateResult contract = Flyway.configure()
            .configuration(flyway.getConfiguration())
            .locations("classpath:db/migration", "classpath:db/contract")
            .dataSource(url, postgres.getUsername(), postgres.getPassword())
            .load()
            .migrate();
        SingleConnectionDataSource contracted =
            new SingleConnectionDataSource(url, postgres.getUsername(), postgres.getPassword(), true);
        List<Map<String, Object>> columns = new JdbcTemplate(contracted).queryForList("""
                select column_name, is_nullable from information_schema.columns
                where table_schema = 'compliance' and table_name = 'sov_processing_activity'
                order by ordinal_position""");
        contracted.destroy();

        System.out.println("=== EXPAND / CONTRACT: controller_contact -> dpo_email ===");
        System.out.println("release 2 reads a release-1 row : " + newReadsOldRow);
        System.out.println("release 1 reads a release-2 row : " + oldReadsNewRow);
        System.out.println("release 1 update seen by 2      : " + newSeesOldUpdate);
        System.out.println("release 2 update seen by 1      : " + oldSeesNewUpdate);
        System.out.println("row loaded without trigger      : dpo_email " + withoutTrigger + ", read as " + formulaFallback);
        System.out.println("contract database               : " + contract.migrationsExecuted + " migrations, now at v"
                + contract.targetSchemaVersion);
        columns.forEach(c -> System.out.println("  column " + c.get("column_name") + " nullable=" + c.get("is_nullable")));

        assertThat(newReadsOldRow).isEqualTo("cctv@sovereign-shop.test");
        assertThat(oldReadsNewRow).isEqualTo("loyalty@sovereign-shop.test");
        assertThat(newSeesOldUpdate).isEqualTo("rewards@sovereign-shop.test");
        assertThat(oldSeesNewUpdate).isEqualTo("dpo@sovereign-shop.test");
        assertThat(withoutTrigger).isEqualTo("(empty)");
        assertThat(formulaFallback).isEqualTo("hr@sovereign-shop.test");
        assertThat(contract.migrationsExecuted).isEqualTo(4);
        assertThat(columns).extracting(c -> c.get("column_name")).doesNotContain("controller_contact")
            .endsWith("dpo_email");
        assertThat(columns).filteredOn(c -> "dpo_email".equals(c.get("column_name")))
            .extracting(c -> c.get("is_nullable")).containsExactly("NO");
    }

    private ProcessingActivity read(String code) {
        return transaction.execute(status -> entityManager.find(ProcessingActivity.class, code));
    }

}
