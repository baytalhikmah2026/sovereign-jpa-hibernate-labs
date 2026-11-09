package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.bench.BenchEmailNotice;
import com.baytalhikmah.sovereign.bench.BenchNotice;
import com.baytalhikmah.sovereign.bench.BenchSmsNotice;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.EmailNotice;
import com.baytalhikmah.sovereign.domain.SmsNotice;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TablePerClassTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void aQueryForTheParentScansEveryChildTable() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new BenchEmailNotice("Your order has shipped", "amina@sovereign.test"));
            entityManager.persist(new BenchSmsNotice("Your order has shipped", "+447700900123"));
            entityManager.persist(new BenchEmailNotice("Spring sale", "bilal@sovereign.test"));
        });

        List<BenchNotice> shipped = entityManager.createQuery(
                "select n from BenchNotice n where n.subject = :subject", BenchNotice.class)
            .setParameter("subject", "Your order has shipped")
            .getResultList();

        System.out.println("=== TABLE PER CLASS (bench) ===");
        System.out.println("Shipping notices found : " + shipped.size());
        assertThat(shipped).hasSize(2);
    }

    @Test
    void separateEntitiesQueryOnlyTheirOwnTable() {
        transaction.executeWithoutResult(status -> {
            Customer layla = new Customer("Layla Hassan", "layla.notices@sovereign.test");
            entityManager.persist(layla);
            entityManager.persist(new EmailNotice(layla, "Your order has shipped", "layla.notices@sovereign.test"));
            entityManager.persist(new SmsNotice(layla, "Your order has shipped", "+447700900456"));
        });

        List<EmailNotice> emails = entityManager.createQuery(
                "select e from EmailNotice e where e.subject = :subject", EmailNotice.class)
            .setParameter("subject", "Your order has shipped")
            .getResultList();

        System.out.println("=== MAPPED SUPERCLASS (shop) ===");
        System.out.println("Email notices found : " + emails.size() + " → " + emails.get(0).getEmailAddress());
        assertThat(emails).hasSize(1);
    }

}
