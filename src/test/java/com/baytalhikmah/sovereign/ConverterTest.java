package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
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
import com.baytalhikmah.sovereign.domain.ProductSpecs;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConverterTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void specsTravelAsJsonbAndEmbeddingsAsVectors() {
        ProductSpecs kettleSpecs = new ProductSpecs("copper", 1700, 1250, List.of("whistle", "induction-ready"));
        float[] kettleEmbedding = { 0.12f, 0.98f, 0.05f };

        UUID kettleId = transaction.execute(status -> {
            Product kettle = new Product("KET-AI", "Copper Kettle", Money.of("39.50", "GBP"));
            kettle.updateCatalogueData(kettleSpecs, kettleEmbedding);
            Product mug = new Product("MUG-AI", "Stoneware Mug", Money.of("6.00", "GBP"));
            mug.updateCatalogueData(new ProductSpecs("stoneware", 350, 400, List.of("dishwasher-safe")),
                new float[] { 0.91f, 0.10f, 0.33f });
            entityManager.persist(kettle);
            entityManager.persist(mug);
            return kettle.getId();
        });

        Product loaded = transaction.execute(status -> entityManager.find(Product.class, kettleId));
        Map<String, Object> row = jdbc.queryForMap("""
            select specs ->> 'material' as material,
                   specs -> 'features' as features,
                   pg_typeof(specs)::text as specs_type,
                   pg_typeof(embedding)::text as embedding_type,
                   vector_dims(embedding) as dimensions
            from sov_product where id = ?""", kettleId);
        String closest = jdbc.queryForObject(
            "select sku from sov_product where embedding is not null order by embedding <-> '[0.10,0.95,0.10]'::vector limit 1",
            String.class);

        System.out.println("=== CONVERTERS ===");
        System.out.println("Specs in Java      : " + loaded.getSpecs());
        System.out.println("Embedding in Java  : " + Arrays.toString(loaded.getEmbedding()));
        System.out.println("Material (SQL)     : " + row.get("material"));
        System.out.println("Features (SQL)     : " + row.get("features"));
        System.out.println("Column types       : " + row.get("specs_type") + ", " + row.get("embedding_type") + "(" + row.get("dimensions") + ")");
        System.out.println("Closest to a kettle-like search : " + closest);

        assertThat(loaded.getSpecs()).isEqualTo(kettleSpecs);
        assertThat(loaded.getEmbedding()).containsExactly(kettleEmbedding);
        assertThat(row.get("material")).isEqualTo("copper");
        assertThat(row.get("specs_type")).isEqualTo("jsonb");
        assertThat(row.get("embedding_type")).isEqualTo("vector");
        assertThat(closest).isEqualTo("KET-AI");
    }

}
