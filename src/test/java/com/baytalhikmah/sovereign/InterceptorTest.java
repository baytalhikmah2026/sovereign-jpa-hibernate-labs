package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.audit.SqlTraceInspector;
import com.baytalhikmah.sovereign.domain.ProductCategory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("sql-trace")
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class InterceptorTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    /** The audit columns of one category, as stored in PostgreSQL. */
    record Stamp(String name, Instant createdAt, Instant updatedAt, String changedBy) {
    }

    @Test
    void everyWriteIsStampedAndTagged(CapturedOutput output) throws Exception {
        UUID teaId = CurrentActor.callAs("catalogue-admin", () -> transaction.execute(status -> {
            ProductCategory tea = new ProductCategory("q66-tea", "Tea");
            entityManager.persist(tea);
            return tea.getId();
        }));
        Stamp created = stamp(teaId);

        Thread.sleep(20);
        CurrentActor.runAs("night-import", () -> transaction.executeWithoutResult(status ->
            entityManager.find(ProductCategory.class, teaId).rename("Loose Tea")));
        Stamp renamed = stamp(teaId);

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session maintenance = sessionFactory.withOptions().noInterceptor().openSession()) {
            maintenance.beginTransaction();
            maintenance.find(ProductCategory.class, teaId).rename("Fine Loose Tea");
            maintenance.getTransaction().commit();
        }
        Stamp afterMaintenance = stamp(teaId);

        String tagged = CurrentActor.callAs("x */ select 1", () -> new SqlTraceInspector().inspect("select 2"));

        System.out.println("=== INTERCEPTOR AND STATEMENT INSPECTOR ===");
        System.out.println("Created          : " + created);
        System.out.println("Renamed          : " + renamed);
        System.out.println("Maintenance      : " + afterMaintenance);
        System.out.println("Unsafe actor     : " + tagged);

        assertThat(created.changedBy()).isEqualTo("catalogue-admin");
        assertThat(created.updatedAt()).isEqualTo(created.createdAt());
        assertThat(renamed.changedBy()).isEqualTo("night-import");
        assertThat(renamed.createdAt()).isEqualTo(created.createdAt());
        assertThat(renamed.updatedAt()).isAfter(created.updatedAt());
        assertThat(afterMaintenance.name()).isEqualTo("Fine Loose Tea");
        assertThat(afterMaintenance.updatedAt()).isEqualTo(renamed.updatedAt());
        assertThat(afterMaintenance.changedBy()).isEqualTo("night-import");
        assertThat(tagged).isEqualTo("/* sovereign actor=x____select_1 */ select 2");
        assertThat(output).contains("| statement | /* sovereign actor=night-import */ select");
        assertThat(output).contains("| statement | /* sovereign actor=system */ select");
    }

    private Stamp stamp(UUID categoryId) {
        return transaction.execute(status -> {
            ProductCategory category = entityManager.find(ProductCategory.class, categoryId);
            return new Stamp(category.getName(), category.getCreatedAt(), category.getUpdatedAt(), category.getChangedBy());
        });
    }

}
