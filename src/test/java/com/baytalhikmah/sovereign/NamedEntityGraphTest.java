package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

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
class NamedEntityGraphTest {

    static final String KEY = "q43";

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Test
    void oneNamedGraphServesAQueryAndAFind() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, KEY));
        String domain = QueryLabData.emailDomain(KEY);
        UUID a1 = entityManager.createQuery("select o.id from PurchaseOrder o where o.orderNumber = :number", UUID.class)
            .setParameter("number", QueryLabData.codePrefix(KEY) + "A1")
            .getSingleResult();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        List<String> lazy = orderOverview.naiveOverview(domain);

        statistics.clear();
        List<String> graph = orderOverview.graphOverview(domain);
        long graphStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        PurchaseOrder order = orderOverview.findForOverview(a1);
        String usedAfterTransaction = order.getCustomer().getFullName() + ", " + order.getLines().size() + " lines";
        long findStatements = statistics.getPrepareStatementCount();

        System.out.println("=== NAMED ENTITY GRAPH ===");
        System.out.println("Graph               : " + PurchaseOrder.OVERVIEW_GRAPH);
        System.out.println("Overview by query   : " + graphStatements + " statement, " + graph.size() + " orders");
        System.out.println("Same screen as lazy : " + graph.equals(lazy));
        System.out.println("Find by id          : " + order.getOrderNumber() + " -> " + usedAfterTransaction
            + " (" + findStatements + " statement)");

        assertThat(graphStatements).isEqualTo(1);
        assertThat(graph).isEqualTo(lazy).hasSize(6);
        assertThat(order.getOrderNumber()).isEqualTo("Q43-A1");
        assertThat(usedAfterTransaction).isEqualTo("Amina Yusuf, 2 lines");
        assertThat(findStatements).isEqualTo(1);
    }

}
