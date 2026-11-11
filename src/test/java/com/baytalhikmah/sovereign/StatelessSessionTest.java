package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.service.CatalogueImport;

import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class StatelessSessionTest {

    static final int ROWS = 2_000;

    @Autowired
    CatalogueImport catalogueImport;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aStatelessSessionMovesRowsWithoutKeepingThem() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        long start = System.nanoTime();
        int managed = catalogueImport.importManaged("Q52-M-", ROWS);
        long managedMillis = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        int sent = catalogueImport.importStateless("Q52-S-", ROWS);
        long statelessMillis = (System.nanoTime() - start) / 1_000_000;

        UUID firstId = jdbc.queryForObject("select id from sov_product where sku = 'Q52-S-00001'", UUID.class);
        statistics.clear();
        boolean sameObject = catalogueImport.sameObjectOnSecondRead(firstId);
        long readStatements = statistics.getPrepareStatementCount();

        Integer managedRows = jdbc.queryForObject("select count(*) from sov_product where sku like 'Q52-M-%'", Integer.class);
        Integer statelessRows = jdbc.queryForObject("select count(*) from sov_product where sku like 'Q52-S-%'", Integer.class);

        System.out.println("=== STATELESS SESSION ===");
        System.out.println("Classic import   : " + managedRows + " rows, " + managed
            + " entities kept in the persistence context, " + managedMillis + " ms");
        System.out.println("Stateless import : " + statelessRows + " rows, " + sent
            + " sent in batches of 50, nothing kept, " + statelessMillis + " ms");
        System.out.println("Same product read twice : same object=" + sameObject + ", " + readStatements + " statements");

        assertThat(managed).isEqualTo(ROWS);
        assertThat(managedRows).isEqualTo(ROWS);
        assertThat(sent).isEqualTo(ROWS);
        assertThat(statelessRows).isEqualTo(ROWS);
        assertThat(sameObject).isFalse();
        assertThat(readStatements).isEqualTo(2);
    }

}
