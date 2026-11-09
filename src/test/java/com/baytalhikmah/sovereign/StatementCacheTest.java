package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.CataloguePager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class StatementCacheTest {

    static final String PREFIX = QueryLabData.codePrefix("q40");

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CataloguePager pager;

    @Test
    void repeatedQueriesAreTranslatedOnceAndPreparedOnTheServer() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q40"));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        pager.firstPage(PREFIX, 10);
        long firstMisses = statistics.getQueryPlanCacheMissCount();
        long firstHits = statistics.getQueryPlanCacheHitCount();

        for (int i = 0; i < 4; i++) {
            pager.firstPage(PREFIX, 10);
        }
        long repeatMisses = statistics.getQueryPlanCacheMissCount() - firstMisses;
        long repeatHits = statistics.getQueryPlanCacheHitCount() - firstHits;

        Long serverPrepared = transaction.execute(status -> {
            for (int i = 0; i < 4; i++) {
                pager.firstPage(PREFIX, 10);
            }
            return jdbc.queryForObject("""
                select count(*) from pg_prepared_statements
                where not from_sql and statement like '%from sov_product p1_0 where p1_0.sku like%'""", Long.class);
        });

        System.out.println("=== STATEMENT CACHING ===");
        System.out.println("Plan cache, first run    : " + firstMisses + " miss(es), " + firstHits + " hit(s)");
        System.out.println("Plan cache, 4 repeats    : " + repeatMisses + " miss(es), " + repeatHits + " hit(s)");
        System.out.println("Server-side prepared     : " + serverPrepared + " statement(s) on this connection");

        assertThat(firstMisses).isPositive();
        assertThat(repeatMisses).isZero();
        assertThat(repeatHits).isGreaterThanOrEqualTo(4);
        assertThat(serverPrepared).isPositive();
    }

}
