package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.CustomerContact;
import com.baytalhikmah.sovereign.query.CustomerDirectory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DtoProjectionTest {

    static final String DOMAIN = QueryLabData.emailDomain("q38");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    CustomerDirectory directory;

    @Test
    void recordsForFixedShapesTuplesForShapesChosenAtRunTime() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q38"));

        List<CustomerContact> contacts = directory.contacts(DOMAIN);
        List<Tuple> nameAndChannel = directory.contactColumns(DOMAIN, List.of("name", "channel"));
        List<Tuple> emailOnly = directory.contactColumns(DOMAIN, List.of("email"));

        System.out.println("=== RECORDS (constructor expression) ===");
        contacts.forEach(contact -> System.out.println("  " + contact));
        System.out.println("=== TUPLES: name + channel ===");
        nameAndChannel.forEach(row -> System.out.println(
            "  " + row.get("name", String.class) + " | " + row.get("channel", String.class)));
        System.out.println("=== TUPLES: email only ===");
        emailOnly.forEach(row -> System.out.println("  " + row.get("email", String.class)));

        assertThat(contacts).hasSize(4);
        assertThat(contacts.getFirst()).isEqualTo(new CustomerContact("Amina Yusuf", "amina" + DOMAIN, "web"));
        assertThat(nameAndChannel.getLast().get("channel", String.class)).isEqualTo("store");
        assertThat(nameAndChannel.getFirst().getElements()).hasSize(2);
        assertThat(emailOnly.getFirst().getElements()).hasSize(1);
    }

}
