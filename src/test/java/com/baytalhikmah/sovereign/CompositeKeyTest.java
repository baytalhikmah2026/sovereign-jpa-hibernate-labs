package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.domain.StockCount;
import com.baytalhikmah.sovereign.domain.StockCountId;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CompositeKeyTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void idClassKeepsTheKeyFieldsFlat() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new InventoryItem("LHR-1", "KET-001", 40));
            entityManager.persist(new InventoryItem("MAN-2", "KET-001", 15));
        });

        InventoryItem leeds = transaction.execute(status -> {
            InventoryItem item = entityManager.find(InventoryItem.class, new InventoryItemId("LHR-1", "KET-001"));
            item.ship(3);
            return item;
        });
        List<InventoryItem> inLondon = entityManager.createQuery(
                "select i from InventoryItem i where i.warehouseCode = :warehouse", InventoryItem.class)
            .setParameter("warehouse", "LHR-1")
            .getResultList();

        System.out.println("=== @IdClass ===");
        System.out.println("Found        : " + leeds.getWarehouseCode() + " / " + leeds.getSku() + " → " + leeds.getQuantityOnHand());
        System.out.println("Key columns  : " + primaryKeyColumns("sov_inventory_item"));

        assertThat(leeds.getQuantityOnHand()).isEqualTo(37);
        assertThat(inLondon).hasSize(1);
    }

    @Test
    void embeddedIdTravelsAsOneValue() {
        StockCountId key = new StockCountId("LHR-1", "KET-001", LocalDate.of(2026, 11, 9));
        transaction.executeWithoutResult(status -> entityManager.persist(new StockCount(key, 38)));

        StockCount found = transaction.execute(status ->
            entityManager.find(StockCount.class, new StockCountId("LHR-1", "KET-001", LocalDate.of(2026, 11, 9))));
        List<StockCount> inLondon = entityManager.createQuery(
                "select s from StockCount s where s.id.warehouseCode = :warehouse", StockCount.class)
            .setParameter("warehouse", "LHR-1")
            .getResultList();

        System.out.println("=== @EmbeddedId ===");
        System.out.println("Found        : " + found.getId() + " → " + found.getCountedQuantity());
        System.out.println("Key columns  : " + primaryKeyColumns("sov_stock_count"));

        assertThat(found.getCountedQuantity()).isEqualTo(38);
        assertThat(inLondon).hasSize(1);
    }

    @Test
    void equalKeysAreInterchangeable() {
        StockCountId first = new StockCountId("LHR-1", "KET-001", LocalDate.of(2026, 11, 9));
        StockCountId second = new StockCountId("LHR-1", "KET-001", LocalDate.of(2026, 11, 9));

        System.out.println("=== EQUALITY ===");
        System.out.println("equals   : " + first.equals(second));
        System.out.println("hashCode : " + (first.hashCode() == second.hashCode()));

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(new InventoryItemId("LHR-1", "KET-001")).isEqualTo(new InventoryItemId("LHR-1", "KET-001"));
    }

    private List<String> primaryKeyColumns(String table) {
        return jdbc.queryForList("""
            select k.column_name
            from information_schema.table_constraints c
            join information_schema.key_column_usage k on k.constraint_name = c.constraint_name
            where c.table_name = ? and c.constraint_type = 'PRIMARY KEY'
            order by k.ordinal_position""", String.class, table);
    }

}
