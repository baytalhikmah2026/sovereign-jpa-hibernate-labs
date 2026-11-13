package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.service.ResilientShipper;
import com.baytalhikmah.sovereign.service.ResilientShipper.Outcome;
import com.baytalhikmah.sovereign.service.StockDesk;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

@Import(ChaosTestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class ChaosTest {

    static final String WAREHOUSE = "Q70-WH";

    static final int AGENTS = 6;

    static final int SHIPMENTS_PER_AGENT = 10;

    static final int START_STOCK = 1000;

    @Autowired
    ResilientShipper shipper;

    @Autowired
    StockDesk stockDesk;

    @Autowired
    Proxy postgresProxy;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void stockStaysExactThroughLatencyCollisionsAndAnOutage() throws Exception {
        stockDesk.register(WAREHOUSE, "KETTLE", START_STOCK);
        postgresProxy.toxics().latency("slow-network", ToxicDirection.DOWNSTREAM, 30).setJitter(20);

        ExecutorService agents = Executors.newFixedThreadPool(AGENTS, Thread.ofPlatform().name("agent-", 1).factory());
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int agent = 1; agent <= AGENTS; agent++) {
            for (int shipment = 1; shipment <= SHIPMENTS_PER_AGENT; shipment++) {
                String shipmentKey = "q70-agent-%d-%02d".formatted(agent, shipment);
                futures.add(agents.submit(() -> shipper.ship(shipmentKey, WAREHOUSE, "KETTLE", 1)));
            }
        }
        Thread outage = Thread.ofPlatform().name("chaos-monkey").start(() -> {
            pause(1500);
            setReachable(false);
            pause(1000);
            setReachable(true);
        });

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(3, TimeUnit.MINUTES));
        }
        outage.join();
        agents.shutdown();
        postgresProxy.toxics().get("slow-network").remove();

        long shippedNow = outcomes.stream().filter(Outcome::shippedNow).count();
        long foundAlreadyShipped = outcomes.size() - shippedNow;
        int attempts = outcomes.stream().mapToInt(Outcome::attempts).sum();
        Tuple receipts = transaction.execute(status -> entityManager.createQuery("""
                select count(r), coalesce(sum(r.quantity), 0) from ShipmentReceipt r
                where r.warehouseCode = :warehouse""", Tuple.class)
            .setParameter("warehouse", WAREHOUSE)
            .getSingleResult());
        int stockLeft = transaction.execute(status -> entityManager
            .find(InventoryItem.class, new InventoryItemId(WAREHOUSE, "KETTLE"))
            .getQuantityOnHand());

        System.out.println("=== CHAOS REPORT ===");
        System.out.println("Shipments requested      : " + outcomes.size());
        System.out.println("Shipped by last attempt  : " + shippedNow);
        System.out.println("Found already shipped    : " + foundAlreadyShipped);
        System.out.println("Attempts in total        : " + attempts + " (" + (attempts - outcomes.size()) + " retries)");
        System.out.println("Receipts in the database : " + receipts.get(0) + " for " + receipts.get(1) + " kettles");
        System.out.println("Stock left               : " + stockLeft + " of " + START_STOCK);

        int requested = AGENTS * SHIPMENTS_PER_AGENT;
        assertThat(outcomes).hasSize(requested);
        assertThat(attempts).isGreaterThan(requested);
        assertThat(receipts.get(0, Long.class)).isEqualTo(requested);
        assertThat(((Number) receipts.get(1)).intValue()).isEqualTo(requested);
        assertThat(stockLeft).isEqualTo(START_STOCK - requested);
    }

    private void setReachable(boolean reachable) {
        try {
            if (reachable) {
                postgresProxy.enable();
                System.out.println("=== chaos-monkey: database reachable again ===");
            }
            else {
                postgresProxy.disable();
                System.out.println("=== chaos-monkey: database unreachable ===");
            }
        }
        catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

}
