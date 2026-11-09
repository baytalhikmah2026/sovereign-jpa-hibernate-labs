package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.query.ServerSideLogic;
import com.baytalhikmah.sovereign.query.TopProduct;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StoredProcedureTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ServerSideLogic serverSide;

    @Test
    void logicInsideTheDatabaseIsCalledFromJava() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q33"));
        UUID aminaId = jdbc.queryForObject("select id from sov_customer where email = ?",
            UUID.class, "amina" + QueryLabData.emailDomain("q33"));

        BigDecimal aminaValue = serverSide.lifetimeValue(aminaId);
        List<TopProduct> top3 = serverSide.topProducts(QueryLabData.codePrefix("q33"), 3);
        serverSide.restock("Q33-WH", "Q33-KET", 10);
        serverSide.restock("Q33-WH", "Q33-KET", 5);
        int onHand = transaction.execute(status ->
            entityManager.find(InventoryItem.class, new InventoryItemId("Q33-WH", "Q33-KET")).getQuantityOnHand());

        System.out.println("=== SERVER-SIDE LOGIC ===");
        System.out.println("Lifetime value of Amina : " + aminaValue);
        top3.forEach(row -> System.out.println("Top product            : " + row.sku() + " | " + row.units() + " units | " + row.revenue()));
        System.out.println("Kettles in Q33-WH       : " + onHand + " (restocked 10, then 5)");

        assertThat(aminaValue).isEqualByComparingTo("174.50");
        assertThat(top3).extracting(TopProduct::sku).containsExactly("Q33-PAN", "Q33-KET", "Q33-MUG");
        assertThat(onHand).isEqualTo(15);
    }

}
