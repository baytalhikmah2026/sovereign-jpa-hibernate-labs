package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.service.OrderOverview;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class JoinFetchTest {

    static final String DOMAIN = QueryLabData.emailDomain("q42");

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Test
    void oneQueryLoadsTheWholeOverview() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q42"));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        List<String> lazy = orderOverview.naiveOverview(DOMAIN);
        long lazyStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        List<String> fetched = orderOverview.joinFetchOverview(DOMAIN);
        long fetchStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        List<PurchaseOrder> orders = orderOverview.ordersWithCustomerAndLines(DOMAIN);
        PurchaseOrder first = orders.getFirst();
        String usedAfterTransaction = first.getCustomer().getFullName() + ", " + first.getLines().size() + " lines";
        long afterTransactionStatements = statistics.getPrepareStatementCount();

        System.out.println("=== JOIN FETCH ===");
        System.out.println("Lazy overview       : " + lazyStatements + " statements");
        System.out.println("Join fetch overview : " + fetchStatements + " statement");
        System.out.println("Same screen         : " + fetched.equals(lazy));
        System.out.println("Orders returned     : " + orders.size() + " (no duplicates from the line join)");
        System.out.println("After transaction   : " + usedAfterTransaction + " (" + afterTransactionStatements + " statement)");

        assertThat(lazyStatements).isEqualTo(10);
        assertThat(fetchStatements).isEqualTo(1);
        assertThat(fetched).isEqualTo(lazy);
        assertThat(orders).hasSize(6);
        assertThat(usedAfterTransaction).isEqualTo("Amina Yusuf, 2 lines");
        assertThat(afterTransactionStatements).isEqualTo(1);
    }

}
