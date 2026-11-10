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
import jakarta.persistence.PersistenceUnitUtil;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DynamicEntityGraphTest {

    static final String KEY = "q44";

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Test
    void eachScreenLoadsExactlyWhatItShows() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, KEY));
        String domain = QueryLabData.emailDomain(KEY);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        PersistenceUnitUtil loaded = entityManagerFactory.getPersistenceUnitUtil();

        statistics.clear();
        List<PurchaseOrder> listScreen = orderOverview.ordersForListScreen(domain);
        long listStatements = statistics.getPrepareStatementCount();
        PurchaseOrder listed = listScreen.getFirst();

        statistics.clear();
        List<PurchaseOrder> detailScreen = orderOverview.ordersForDetailScreen(domain);
        long detailStatements = statistics.getPrepareStatementCount();
        PurchaseOrder detailed = detailScreen.getFirst();
        List<String> products = detailed.getLines().stream()
            .map(line -> line.getProduct().getName())
            .sorted()
            .toList();
        long afterTransactionStatements = statistics.getPrepareStatementCount() - detailStatements;

        System.out.println("=== DYNAMIC ENTITY GRAPHS ===");
        System.out.println("List screen   : " + listStatements + " statement, " + listScreen.size() + " orders, "
            + listed.getOrderNumber() + " | " + listed.getCustomer().getFullName()
            + " | lines loaded: " + loaded.isLoaded(listed, "lines"));
        System.out.println("Detail screen : " + detailStatements + " statement, " + detailScreen.size() + " orders, "
            + detailed.getOrderNumber() + " -> " + products);
        System.out.println("After the transaction: " + afterTransactionStatements + " extra statements");

        assertThat(listStatements).isEqualTo(1);
        assertThat(listScreen).hasSize(6);
        assertThat(listed.getCustomer().getFullName()).isEqualTo("Amina Yusuf");
        assertThat(loaded.isLoaded(listed, "lines")).isFalse();
        assertThat(detailStatements).isEqualTo(1);
        assertThat(detailScreen).hasSize(6);
        assertThat(products).containsExactly("Copper Kettle", "Stoneware Mug");
        assertThat(afterTransactionStatements).isZero();
    }

}
