package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductCategory;
import com.baytalhikmah.sovereign.domain.ProductReview;
import com.baytalhikmah.sovereign.service.ProductShowcase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MultipleBagFetchTest {

    static final String KEY = "q45";
    static final String PREFIX = QueryLabData.codePrefix(KEY);

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ProductShowcase showcase;

    @Test
    void twoBagsNeedTwoQueries() {
        transaction.executeWithoutResult(status -> createShowcaseData());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        assertThatThrownBy(() -> showcase.inOneQuery(PREFIX))
            .satisfies(error -> assertThat(rootCause(error)).isInstanceOf(MultipleBagFetchException.class));

        Integer oneQueryRows = jdbc.queryForObject("""
                select count(*) from sov_product p
                left join sov_product_category_link l on l.product_id = p.id
                left join sov_product_review r on r.product_id = p.id and r.deleted_at is null
                where p.sku = ?""", Integer.class, PREFIX + "KET");
        Integer linkRows = jdbc.queryForObject(
            "select count(*) from sov_product p join sov_product_category_link l on l.product_id = p.id where p.sku = ?",
            Integer.class, PREFIX + "KET");
        Integer reviewRows = jdbc.queryForObject(
            "select count(*) from sov_product p join sov_product_review r on r.product_id = p.id where p.sku = ?",
            Integer.class, PREFIX + "KET");

        statistics.clear();
        List<Product> products = showcase.inTwoQueries(PREFIX);
        long statements = statistics.getPrepareStatementCount();
        Product kettle = products.stream().filter(p -> p.getSku().equals(PREFIX + "KET")).findFirst().orElseThrow();

        System.out.println("=== MULTIPLE BAGS ===");
        System.out.println("One query, two bags : refused (MultipleBagFetchException)");
        System.out.println("Kettle rows if joined together : " + oneQueryRows + " (3 links x 4 reviews)");
        System.out.println("Kettle rows in two queries     : " + (linkRows + reviewRows) + " (3 + 4)");
        System.out.println("Two queries         : " + statements + " statements, " + products.size() + " products");
        System.out.println("Kettle after tx     : " + kettle.getCategoryLinks().size() + " categories, "
            + kettle.getReviews().size() + " reviews");

        assertThat(oneQueryRows).isEqualTo(12);
        assertThat(linkRows + reviewRows).isEqualTo(7);
        assertThat(statements).isEqualTo(2);
        assertThat(products).hasSize(6);
        assertThat(kettle.getCategoryLinks()).hasSize(3);
        assertThat(kettle.getReviews()).hasSize(4);
    }

    private void createShowcaseData() {
        QueryLabData.create(entityManager, KEY);
        Product kettle = product("KET");
        Product mug = product("MUG");
        ProductCategory kitchen = new ProductCategory(KEY + "-kitchen", "Kitchen");
        ProductCategory gifts = new ProductCategory(KEY + "-gifts", "Gifts");
        ProductCategory copper = new ProductCategory(KEY + "-copper", "Copper");
        List.of(kitchen, gifts, copper).forEach(entityManager::persist);
        kettle.addToCategory(kitchen, true, 1);
        kettle.addToCategory(gifts, false, 2);
        kettle.addToCategory(copper, false, 3);
        mug.addToCategory(kitchen, true, 2);
        String[] reviewers = { "amina", "bilal", "zara", "omar" };
        int[] ratings = { 5, 4, 5, 3 };
        for (int i = 0; i < reviewers.length; i++) {
            entityManager.persist(new ProductReview(kettle, customer(reviewers[i]), ratings[i], "Kettle review " + (i + 1)));
        }
        entityManager.persist(new ProductReview(mug, customer("amina"), 4, "Solid mug"));
        entityManager.persist(new ProductReview(mug, customer("zara"), 5, "Keeps tea warm"));
        entityManager.flush();
        entityManager.clear();
    }

    private Product product(String code) {
        return entityManager.createQuery("select p from Product p where p.sku = :sku", Product.class)
            .setParameter("sku", PREFIX + code)
            .getSingleResult();
    }

    private Customer customer(String name) {
        return entityManager.createQuery("select c from Customer c where c.email = :email", Customer.class)
            .setParameter("email", name + QueryLabData.emailDomain(KEY))
            .getSingleResult();
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

}
