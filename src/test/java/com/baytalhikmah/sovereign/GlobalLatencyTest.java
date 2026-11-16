package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.testcontainers.cockroachdb.CockroachContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import com.baytalhikmah.sovereign.persistence.GlobalPersistenceUnit;
import com.baytalhikmah.sovereign.persistence.NearestEndpointDataSource;
import com.baytalhikmah.sovereign.persistence.TransactionRetry;
import com.baytalhikmah.sovereign.service.GlobalStockDesk;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;

class GlobalLatencyTest {

    private static final Map<String, Integer> DISTANCE_MS = Map.of("eu-west", 0, "us-east", 80, "ap-south", 150);

    @Test
    void theApplicationTalksToTheNearestNodeAndReadsNearbyCopies() throws Exception {
        try (Network network = Network.newNetwork();
                CockroachContainer cockroach = new CockroachContainer("cockroachdb/cockroach:v26.3.5")
                    .withNetwork(network)
                    .withNetworkAliases("cockroach");
                ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
                    DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.13.0")).withNetwork(network)) {
            cockroach.start();
            toxiproxy.start();
            ToxiproxyClient client = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());

            Map<String, DataSource> gateways = new LinkedHashMap<>();
            Map<String, Proxy> links = new LinkedHashMap<>();
            int port = 8666;
            for (String region : new String[] { "ap-south", "us-east", "eu-west" }) {
                Proxy link = client.createProxy(region, "0.0.0.0:" + port, "cockroach:26257");
                if (DISTANCE_MS.get(region) > 0) {
                    link.toxics().latency("distance", ToxicDirection.DOWNSTREAM, DISTANCE_MS.get(region));
                }
                links.put(region, link);
                String url = "jdbc:postgresql://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(port)
                        + "/postgres?sslmode=disable";
                gateways.put(region, GlobalPersistenceUnit.pool("Global-" + region, url, cockroach.getUsername(),
                    cockroach.getPassword()));
                port++;
            }

            try (NearestEndpointDataSource router = new NearestEndpointDataSource(gateways);
                    GlobalPersistenceUnit global = new GlobalPersistenceUnit(router)) {
                Map<String, Duration> firstLook = router.refresh();
                String firstChoice = router.nearest();

                links.get("eu-west").toxics().latency("outage-lag", ToxicDirection.DOWNSTREAM, 300);
                Map<String, Duration> secondLook = router.refresh();
                String secondChoice = router.nearest();
                links.get("eu-west").toxics().get("outage-lag").remove();
                router.refresh();

                GlobalStockDesk desk = new GlobalStockDesk(
                    new TransactionRetry(global.entityManagerFactory(), 5, Duration.ofMillis(50)));
                desk.register("Q98-LANTERN", "ap-south", "IN", 30);
                Thread.sleep(6_000);
                desk.reserve("Q98-LANTERN", "ap-south", 1);
                int current = desk.quantity("Q98-LANTERN", "ap-south");
                int followerNow = desk.quantityNearby("Q98-LANTERN", "ap-south");
                Thread.sleep(6_000);
                int followerLater = desk.quantityNearby("Q98-LANTERN", "ap-south");

                System.out.println("=== GLOBAL READ LATENCY ===");
                System.out.println("round trips        : " + NearestEndpointDataSource.inMillis(firstLook));
                System.out.println("nearest endpoint   : " + firstChoice);
                System.out.println("eu-west slows down : " + NearestEndpointDataSource.inMillis(secondLook));
                System.out.println("nearest endpoint   : " + secondChoice);
                System.out.println("current stock      : " + current);
                System.out.println("follower read now  : " + followerNow + " (a few seconds old)");
                System.out.println("follower read +6 s : " + followerLater);

                assertThat(firstChoice).isEqualTo("eu-west");
                assertThat(firstLook.get("ap-south")).isGreaterThan(firstLook.get("us-east"));
                assertThat(firstLook.get("us-east")).isGreaterThan(firstLook.get("eu-west"));
                assertThat(secondChoice).isEqualTo("us-east");
                assertThat(router.nearest()).isEqualTo("eu-west");
                assertThat(current).isEqualTo(29);
                assertThat(followerNow).isEqualTo(30);
                assertThat(followerLater).isEqualTo(29);
            }
        }
    }

}
