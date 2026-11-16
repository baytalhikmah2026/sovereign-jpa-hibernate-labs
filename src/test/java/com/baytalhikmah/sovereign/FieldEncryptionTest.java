package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.crypto.AEADBadTagException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.CustomerIdentity;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FieldEncryptionTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theDatabaseSeesOnlyCipherTextAndTheApplicationSeesPlainText() {
        UUID[] ids = transaction.execute(status -> {
            Customer omar = new Customer("Omar Siddiqui", "omar@q91.shop.test");
            Customer nadia = new Customer("Nadia Siddiqui", "nadia@q91.shop.test");
            entityManager.persist(omar);
            entityManager.persist(nadia);
            CustomerIdentity first = new CustomerIdentity(omar, "PASSPORT", "K12345678", LocalDate.of(1990, 4, 12));
            CustomerIdentity second = new CustomerIdentity(nadia, "PASSPORT", "K12345678", LocalDate.of(1992, 9, 3));
            entityManager.persist(first);
            entityManager.persist(second);
            return new UUID[] { first.getId(), second.getId(), omar.getId() };
        });

        List<String> stored = jdbc.queryForList(
            "select document_number from sov_customer_identity where id in (?, ?) order by id", String.class,
            ids[0], ids[1]);
        CustomerIdentity readBack = transaction.execute(status -> entityManager.find(CustomerIdentity.class, ids[0]));

        jdbc.update("""
                insert into sov_customer_identity (id, customer_id, document_type, document_number, date_of_birth)
                values (?, ?, 'ID_CARD', 'LEGACY-001', '1985-01-30')""", UUID.randomUUID(), ids[2]);
        String legacy = transaction.execute(status -> entityManager.createQuery(
                "select i from CustomerIdentity i where i.documentType = 'ID_CARD' and i.customer.id = :customer",
                CustomerIdentity.class).setParameter("customer", ids[2]).getSingleResult().getDocumentNumber());

        jdbc.update("""
                update sov_customer_identity
                set document_number = overlay(document_number placing
                    (case when substr(document_number, 20, 1) = 'A' then 'B' else 'A' end) from 20 for 1)
                where id = ?""", ids[1]);
        Throwable tampered = catchThrowable(() ->
            transaction.execute(status -> entityManager.find(CustomerIdentity.class, ids[1]).getDocumentNumber()));

        System.out.println("=== FIELD ENCRYPTION ===");
        System.out.println("stored (Omar)   : " + stored.get(0).substring(0, 30) + "... (" + stored.get(0).length() + " characters)");
        System.out.println("stored (Nadia)  : " + stored.get(1).substring(0, 30) + "... (" + stored.get(1).length() + " characters)");
        System.out.println("read by the app : " + readBack.getDocumentNumber() + ", born " + readBack.getDateOfBirth());
        System.out.println("legacy row      : " + legacy);
        System.out.println("tampered row    : " + tampered.getClass().getSimpleName());

        assertThat(stored).allMatch(value -> value.startsWith("enc:v1:") && !value.contains("K12345678"));
        assertThat(stored.get(0)).isNotEqualTo(stored.get(1));
        assertThat(readBack.getDocumentNumber()).isEqualTo("K12345678");
        assertThat(readBack.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 4, 12));
        assertThat(legacy).isEqualTo("LEGACY-001");
        assertThat(tampered).hasRootCauseInstanceOf(AEADBadTagException.class);
    }

}
