package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.query.SemanticCatalogue;
import com.baytalhikmah.sovereign.query.SimilarProduct;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class VectorSearchTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SemanticCatalogue catalogue;

    @Test
    void agentsFindProductsByMeaning() {
        transaction.executeWithoutResult(status -> {
            save("Q96-KETTLE-STEEL", "Steel Kettle", "34.00", 0.90f, 0.10f, 0.00f);
            save("Q96-KETTLE-GLASS", "Glass Kettle", "29.00", 0.85f, 0.15f, 0.05f);
            save("Q96-TEAPOT", "Ceramic Teapot", "22.00", 0.70f, 0.30f, 0.10f);
            save("Q96-HEADPHONES", "Studio Headphones", "89.00", 0.10f, 0.90f, 0.20f);
            save("Q96-DESK-LAMP", "Desk Lamp", "45.00", 0.05f, 0.10f, 0.95f);
        });

        float[] boilWater = { 0.88f, 0.12f, 0.02f };
        List<SimilarProduct> answer = catalogue.search("Q96-", boilWater, 3);
        List<SimilarProduct> alsoLike = catalogue.moreLike("Q96-KETTLE-STEEL", 2);

        List<String> plan = transaction.execute(status -> {
            jdbc.execute("set local enable_seqscan = off");
            return jdbc.queryForList("""
                    explain select sku from sov_product
                    order by embedding <=> '[0.88,0.12,0.02]'::vector limit 3""", String.class);
        });
        String index = jdbc.queryForObject(
            "select indexdef from pg_indexes where indexname = 'sov_product_embedding_hnsw'", String.class);

        System.out.println("=== SEMANTIC SEARCH ===");
        System.out.println("question \"something to boil water\":");
        answer.forEach(hit -> System.out.printf("  %-18s %-16s distance %.3f%n", hit.sku(), hit.name(), hit.distance()));
        System.out.println("more like Q96-KETTLE-STEEL:");
        alsoLike.forEach(hit -> System.out.printf("  %-18s %-16s distance %.3f%n", hit.sku(), hit.name(), hit.distance()));
        System.out.println("index    : " + index);
        System.out.println("plan     : " + plan.stream().filter(line -> line.contains("Index Scan")).findFirst().orElse("-").strip());

        assertThat(answer).extracting(SimilarProduct::sku)
            .containsExactly("Q96-KETTLE-STEEL", "Q96-KETTLE-GLASS", "Q96-TEAPOT");
        assertThat(answer).extracting(SimilarProduct::distance).isSorted();
        assertThat(alsoLike).extracting(SimilarProduct::sku).containsExactly("Q96-KETTLE-GLASS", "Q96-TEAPOT");
        assertThat(index).contains("USING hnsw").contains("vector_cosine_ops");
        assertThat(plan).anyMatch(line -> line.contains("Index Scan using sov_product_embedding_hnsw"));
    }

    private void save(String sku, String name, String price, float... embedding) {
        Product product = new Product(sku, name, Money.of(price, "EUR"));
        product.updateCatalogueData(null, embedding);
        entityManager.persist(product);
    }

}
