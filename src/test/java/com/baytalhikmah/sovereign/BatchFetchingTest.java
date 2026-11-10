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

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductReview;
import com.baytalhikmah.sovereign.service.OrderOverview;
import com.baytalhikmah.sovereign.service.ProductShowcase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.default_batch_fetch_size=16" })
class BatchFetchingTest {

    static final String KEY = "q46";
    static final String REVIEWED_PREFIX = QueryLabData.codePrefix(KEY) + "R";

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Autowired
    ProductShowcase showcase;

    @Test
    void lazyLoadingInBatches() {
        transaction.executeWithoutResult(status -> {
            QueryLabData.create(entityManager, KEY);
            createReviewedProducts(20);
        });
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        List<String> overview = orderOverview.naiveOverview(QueryLabData.emailDomain(KEY));
        long overviewStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        List<String> reviewCounts = showcase.reviewCounts(REVIEWED_PREFIX);
        long reviewStatements = statistics.getPrepareStatementCount();

        System.out.println("=== BATCH FETCHING ===");
        System.out.println("Global batch size   : 16");
        System.out.println("Lazy order overview : " + overview.size() + " orders, " + overviewStatements
            + " statements (orders, customers, lines)");
        System.out.println("Review counts       : " + reviewCounts.size() + " products, " + reviewStatements
            + " statements (@BatchSize 25 on reviews)");
        System.out.println("First / last        : " + reviewCounts.getFirst() + " / " + reviewCounts.getLast());

        assertThat(overview).hasSize(6);
        assertThat(overviewStatements).isEqualTo(3);
        assertThat(reviewCounts).hasSize(20).allMatch(line -> line.endsWith(": 1 reviews"));
        assertThat(reviewStatements).isEqualTo(2);
    }

    private void createReviewedProducts(int count) {
        Customer amina = entityManager.createQuery("select c from Customer c where c.email = :email", Customer.class)
            .setParameter("email", "amina" + QueryLabData.emailDomain(KEY))
            .getSingleResult();
        for (int i = 1; i <= count; i++) {
            Product product = new Product(REVIEWED_PREFIX + String.format("%02d", i), "Reviewed Item " + i,
                Money.of("10.00", "GBP"));
            entityManager.persist(product);
            entityManager.persist(new ProductReview(product, amina, 4, "Review of item " + i));
        }
        entityManager.flush();
        entityManager.clear();
    }

}
