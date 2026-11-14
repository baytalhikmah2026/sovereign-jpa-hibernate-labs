package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.query.CatalogueSearch;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("search")
@SpringBootTest
class SearchShardingTest {

    @Autowired
    CatalogueSearch catalogueSearch;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void searchesTheShardedCatalogueIndex() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> Stream.of(
                new Product("KIT-Q76-KETTLE", "Steel Kettle", Money.of("39.00", "EUR")),
                new Product("KIT-Q76-TEAPOT", "Glass Teapot", Money.of("24.00", "EUR")),
                new Product("KIT-Q76-PAN", "Steel Frying Pan", Money.of("45.00", "EUR")),
                new Product("GAR-Q76-HOSE", "Garden Hose", Money.of("29.00", "EUR")),
                new Product("GAR-Q76-SHEARS", "Steel Pruning Shears", Money.of("19.00", "EUR")),
                new Product("OFF-Q76-LAMP", "Desk Lamp", Money.of("35.00", "EUR")),
                new Product("BTH-Q76-RACK", "Steel Towel Rack", Money.of("27.00", "EUR")))
            .forEach(entityManager::persist));

        List<String> steelEverywhere = catalogueSearch.findNames("steel");
        List<String> steelInKitchen = catalogueSearch.findNamesInDepartment("KIT", "steel");
        Map<String, Long> documentsPerShard = new LinkedHashMap<>();
        for (String department : List.of("KIT", "GAR", "OFF", "BTH")) {
            documentsPerShard.put(department, catalogueSearch.countInShardOf(department));
        }
        List<String> shardFolders;
        try (Stream<Path> folders = Files.list(Path.of("target", "search-index", "products"))) {
            shardFolders = folders.filter(Files::isDirectory).map(folder -> folder.getFileName().toString())
                .sorted().toList();
        }

        jdbcTemplate.update("update sov_product set name = 'Copper Kettle' where sku = 'KIT-Q76-KETTLE'");
        List<String> copperBeforeRebuild = catalogueSearch.findNames("copper");
        catalogueSearch.rebuildIndex();
        List<String> copperAfterRebuild = catalogueSearch.findNames("copper");

        System.out.println("=== SHARDED CATALOGUE SEARCH ===");
        System.out.println("\"steel\" on all shards   : " + steelEverywhere);
        System.out.println("\"steel\" in KIT only     : " + steelInKitchen);
        System.out.println("documents in the shard : " + documentsPerShard);
        System.out.println("shard folders          : " + shardFolders);
        System.out.println("\"copper\" before rebuild : " + copperBeforeRebuild);
        System.out.println("\"copper\" after rebuild  : " + copperAfterRebuild);

        assertThat(steelEverywhere)
            .containsExactly("Steel Towel Rack", "Steel Pruning Shears", "Steel Kettle", "Steel Frying Pan");
        assertThat(steelInKitchen).containsExactly("Steel Kettle", "Steel Frying Pan");
        assertThat(documentsPerShard).containsExactly(Map.entry("KIT", 4L), Map.entry("GAR", 2L),
            Map.entry("OFF", 1L), Map.entry("BTH", 4L));
        assertThat(shardFolders).containsExactly("0", "1", "2", "3");
        assertThat(copperBeforeRebuild).isEmpty();
        assertThat(copperAfterRebuild).containsExactly("Copper Kettle");
    }

}
