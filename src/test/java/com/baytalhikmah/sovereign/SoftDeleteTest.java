package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductReview;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SoftDeleteTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aDeletedReviewDisappearsFromTheShopButStaysOnRecord() {
        UUID[] ids = transaction.execute(status -> {
            Product kettle = new Product("KET-REV", "Copper Kettle", Money.of("39.50", "GBP"));
            Customer amina = new Customer("Amina Yusuf", "amina.reviews@sovereign.test");
            Customer bilal = new Customer("Bilal Ahmed", "bilal.reviews@sovereign.test");
            entityManager.persist(kettle);
            entityManager.persist(amina);
            entityManager.persist(bilal);
            ProductReview good = new ProductReview(kettle, amina, 5, "Boils fast and looks beautiful.");
            ProductReview spam = new ProductReview(kettle, bilal, 1, "Visit my site for cheap kettles!!!");
            entityManager.persist(good);
            entityManager.persist(spam);
            return new UUID[] { kettle.getId(), spam.getId() };
        });

        transaction.executeWithoutResult(status ->
            entityManager.remove(entityManager.find(ProductReview.class, ids[1])));

        Long visible = entityManager.createQuery(
                "select count(r) from ProductReview r where r.product.id = :productId", Long.class)
            .setParameter("productId", ids[0])
            .getSingleResult();
        ProductReview foundAgain = transaction.execute(status -> entityManager.find(ProductReview.class, ids[1]));
        Integer onRecord = jdbc.queryForObject(
            "select count(*) from sov_product_review where product_id = ?", Integer.class, ids[0]);
        String archived = jdbc.queryForObject(
            "select rating || ' star, deleted_at set: ' || (deleted_at is not null) from sov_product_review where id = ?",
            String.class, ids[1]);

        System.out.println("=== SOFT DELETE ===");
        System.out.println("Reviews the shop shows : " + visible);
        System.out.println("find() after delete    : " + foundAgain);
        System.out.println("Rows on record         : " + onRecord);
        System.out.println("Spam review row        : " + archived);

        assertThat(visible).isEqualTo(1);
        assertThat(foundAgain).isNull();
        assertThat(onRecord).isEqualTo(2);
        assertThat(archived).isEqualTo("1 star, deleted_at set: true");
    }

}
