package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.service.StockDesk;
import com.baytalhikmah.sovereign.service.StockTransfer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PropagationTest {

    static final String FROM = "Q68-A";

    static final String TO = "Q68-B";

    @Autowired
    StockTransfer stockTransfer;

    @Autowired
    StockDesk stockDesk;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void onlyAnIndependentTransactionSurvivesTheRollback() {
        stockDesk.register(FROM, "KETTLE", 10);

        CurrentActor.runAs("ops-amina", () -> {
            assertThatThrownBy(() -> stockTransfer.transfer(FROM, TO, "KETTLE", 4))
                .hasMessage("No stock record of KETTLE in Q68-B");
            assertThatThrownBy(() -> stockTransfer.transferLoggedInCallerTransaction(FROM, TO, "KETTLE", 4))
                .hasMessage("No stock record of KETTLE in Q68-B");
            assertThatThrownBy(() -> stockTransfer.transferWithSelfInvokedLog(FROM, TO, "KETTLE", 4))
                .hasMessage("No stock record of KETTLE in Q68-B");
        });
        int sourceAfterFailures = quantity(FROM);

        stockDesk.register(TO, "KETTLE", 0);
        CurrentActor.runAs("ops-amina", () -> stockTransfer.transfer(FROM, TO, "KETTLE", 4));
        Map<String, Long> entries = logEntriesPerAction();

        System.out.println("=== TRANSACTION PROPAGATION ===");
        System.out.println("Source after 3 failed transfers : " + sourceAfterFailures);
        System.out.println("After the successful transfer   : " + FROM + "=" + quantity(FROM) + ", " + TO + "=" + quantity(TO));
        System.out.println("Log entries per action          : " + entries);

        assertThat(sourceAfterFailures).isEqualTo(10);
        assertThat(quantity(FROM)).isEqualTo(6);
        assertThat(quantity(TO)).isEqualTo(4);
        assertThat(entries).containsExactly(Map.entry("transfer", 2L));
    }

    private int quantity(String warehouseCode) {
        return transaction.execute(status -> entityManager
            .find(InventoryItem.class, new InventoryItemId(warehouseCode, "KETTLE"))
            .getQuantityOnHand());
    }

    private Map<String, Long> logEntriesPerAction() {
        return transaction.execute(status -> {
            Map<String, Long> perAction = new TreeMap<>();
            entityManager.createQuery("""
                    select e.action, count(e) from ActionLogEntry e
                    where e.detail like 'Q68-A%' and e.actor = 'ops-amina'
                    group by e.action""", Tuple.class)
                .getResultList()
                .forEach(row -> perAction.put(row.get(0, String.class), row.get(1, Long.class)));
            return perAction;
        });
    }

}
