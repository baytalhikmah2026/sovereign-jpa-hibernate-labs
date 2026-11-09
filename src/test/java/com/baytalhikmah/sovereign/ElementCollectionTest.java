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

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ElementCollectionTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void imagesKeepTheirOrderAndChangeRowByRow() {
        UUID id = transaction.execute(status -> {
            Product kettle = new Product("KET-IMG", "Copper Kettle", Money.of("39.50", "GBP"));
            kettle.addImage("https://img.sovereign.test/ket/front.jpg");
            kettle.addImage("https://img.sovereign.test/ket/side.jpg");
            kettle.addImage("https://img.sovereign.test/ket/lid.jpg");
            kettle.addImage("https://img.sovereign.test/ket/box.jpg");
            entityManager.persist(kettle);
            return kettle.getId();
        });

        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, id).removeImage("https://img.sovereign.test/ket/box.jpg"));
        long recreatedAfterRemove = statistics.getCollectionRecreateCount();
        long updatedAfterRemove = statistics.getCollectionUpdateCount();

        List<String> rows = jdbc.queryForList(
            "select position || ': ' || image_url from sov_product_image where product_id = ? order by position",
            String.class, id);
        List<String> indexes = jdbc.queryForList(
            "select indexdef from pg_indexes where tablename = 'sov_product_image'", String.class);

        System.out.println("=== ELEMENT COLLECTION ===");
        rows.forEach(row -> System.out.println("  " + row));
        System.out.println("Collection recreated : " + recreatedAfterRemove + " times");
        System.out.println("Collection updated   : " + updatedAfterRemove + " times");
        System.out.println("Index                : " + indexes);

        assertThat(rows).containsExactly(
            "0: https://img.sovereign.test/ket/front.jpg",
            "1: https://img.sovereign.test/ket/side.jpg",
            "2: https://img.sovereign.test/ket/lid.jpg");
        assertThat(recreatedAfterRemove).isZero();
        assertThat(updatedAfterRemove).isEqualTo(1);
    }

}
