package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductReview;
import com.baytalhikmah.sovereign.domain.ProductReview_;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class LazyPropertyTest {

    static final String KEY = "q48";
    static final String LONG_COMMENT = "Brews fast. ".repeat(125);

    record Reading(boolean enhanced, boolean loadedByQuery, long queryStatements,
            int commentLength, boolean loadedAfterRead, long totalStatements) {
    }

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void theCommentWaitsUntilItIsRead() {
        transaction.executeWithoutResult(status -> createReview());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        Reading reading = transaction.execute(status -> {
            statistics.clear();
            ProductReview review = entityManager.createQuery(
                    "select r from ProductReview r where r.product.sku = :sku", ProductReview.class)
                .setParameter("sku", QueryLabData.codePrefix(KEY) + "KET")
                .getSingleResult();
            boolean loadedByQuery = Hibernate.isPropertyInitialized(review, ProductReview_.comment);
            long queryStatements = statistics.getPrepareStatementCount();
            int commentLength = review.getComment().length();
            return new Reading(review instanceof PersistentAttributeInterceptable, loadedByQuery, queryStatements,
                commentLength, Hibernate.isPropertyInitialized(review, ProductReview_.comment),
                statistics.getPrepareStatementCount());
        });

        System.out.println("=== LAZY PROPERTY ===");
        System.out.println("Enhanced class          : " + reading.enhanced());
        System.out.println("Comment after the query : loaded=" + reading.loadedByQuery()
            + " (" + reading.queryStatements() + " statement)");
        System.out.println("Comment after reading   : loaded=" + reading.loadedAfterRead()
            + ", " + reading.commentLength() + " characters (" + reading.totalStatements() + " statements in total)");

        assertThat(reading.enhanced()).isTrue();
        assertThat(reading.loadedByQuery()).isFalse();
        assertThat(reading.queryStatements()).isEqualTo(1);
        assertThat(reading.commentLength()).isEqualTo(1500);
        assertThat(reading.loadedAfterRead()).isTrue();
        assertThat(reading.totalStatements()).isEqualTo(2);
    }

    private void createReview() {
        QueryLabData.create(entityManager, KEY);
        Product kettle = entityManager.createQuery("select p from Product p where p.sku = :sku", Product.class)
            .setParameter("sku", QueryLabData.codePrefix(KEY) + "KET")
            .getSingleResult();
        Customer amina = entityManager.createQuery("select c from Customer c where c.email = :email", Customer.class)
            .setParameter("email", "amina" + QueryLabData.emailDomain(KEY))
            .getSingleResult();
        entityManager.persist(new ProductReview(kettle, amina, 5, LONG_COMMENT));
        entityManager.flush();
        entityManager.clear();
    }

}
