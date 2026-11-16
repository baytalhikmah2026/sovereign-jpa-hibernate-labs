package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
@SpringBootTest
class EnversAuditTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyCommittedChangeLeavesARowInTheHistoryTable() {
        UUID id = transaction.execute(status -> {
            Product lamp = new Product("Q87-LAMP", "Desk Lamp", Money.of("40.00", "EUR"));
            entityManager.persist(lamp);
            return lamp.getId();
        });
        transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, id).changePrice(Money.of("45.00", "EUR")));
        transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, id).rename("Brass Desk Lamp"));
        transaction.executeWithoutResult(status ->
            entityManager.remove(entityManager.find(Product.class, id)));

        List<Map<String, Object>> history = jdbc.queryForList("""
                select rev, revtype, sku, name, price_amount
                from sov_product_aud
                where id = ?
                order by rev""", id);
        List<String> historyTables = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_name like '%aud'
                order by table_name""", String.class);

        System.out.println("=== PRODUCT HISTORY ===");
        System.out.println("tables : " + historyTables);
        history.forEach(row -> System.out.println("rev " + row.get("rev") + " | type " + row.get("revtype")
                + " | " + row.get("sku") + " | " + row.get("name") + " | " + row.get("price_amount")));

        assertThat(history).extracting(row -> ((Number) row.get("revtype")).intValue()).containsExactly(0, 1, 1, 2);
        assertThat(history).extracting(row -> row.get("name"))
            .containsExactly("Desk Lamp", "Desk Lamp", "Brass Desk Lamp", "Brass Desk Lamp");
        assertThat(historyTables).contains("sov_product_aud", "sov_product_image_aud");
    }

}
