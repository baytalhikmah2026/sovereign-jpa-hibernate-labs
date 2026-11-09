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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductCategory;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ManyToManyTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theLinkIsAnEntityWithItsOwnInformation() {
        UUID[] ids = transaction.execute(status -> {
            ProductCategory kitchen = new ProductCategory("kitchen", "Kitchen");
            ProductCategory gifts = new ProductCategory("gifts", "Gifts");
            Product kettle = new Product("KET-M2M", "Copper Kettle", Money.of("39.50", "GBP"));
            Product teapot = new Product("TEA-M2M", "Glass Teapot", Money.of("24.00", "GBP"));
            entityManager.persist(kitchen);
            entityManager.persist(gifts);
            entityManager.persist(kettle);
            entityManager.persist(teapot);

            kettle.addToCategory(kitchen, true, 2);
            kettle.addToCategory(gifts, false, 5);
            teapot.addToCategory(kitchen, true, 1);
            return new UUID[] { kettle.getId(), gifts.getId() };
        });

        List<String> kitchenShelf = entityManager.createQuery("""
                select l.product.name from ProductCategoryLink l
                where l.category.slug = 'kitchen' order by l.displayOrder""", String.class)
            .getResultList();
        List<String> linkColumns = jdbc.queryForList("""
            select column_name from information_schema.columns
            where table_name = 'sov_product_category_link' order by ordinal_position""", String.class);

        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        transaction.executeWithoutResult(status -> {
            Product kettle = entityManager.find(Product.class, ids[0]);
            kettle.removeFromCategory(entityManager.find(ProductCategory.class, ids[1]));
        });
        long deletes = statistics.getEntityDeleteCount();
        long inserts = statistics.getEntityInsertCount();
        Integer kettleLinks = jdbc.queryForObject(
            "select count(*) from sov_product_category_link where product_id = ?", Integer.class, ids[0]);

        System.out.println("=== EXPLICIT LINK ENTITY ===");
        System.out.println("Kitchen shelf      : " + kitchenShelf);
        System.out.println("Link table columns : " + linkColumns);
        System.out.println("Removal            : " + deletes + " row deleted, " + inserts + " rows inserted");
        System.out.println("Kettle links left  : " + kettleLinks);

        assertThat(kitchenShelf).containsExactly("Glass Teapot", "Copper Kettle");
        assertThat(linkColumns).contains("primary_category", "display_order");
        assertThat(deletes).isEqualTo(1);
        assertThat(inserts).isZero();
        assertThat(kettleLinks).isEqualTo(1);
    }

}
