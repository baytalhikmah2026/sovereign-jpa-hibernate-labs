package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.query.HintedQueries;
import com.baytalhikmah.sovereign.query.PlanInspector;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.use_sql_comments=true")
class QueryHintTest {

    static final String PREFIX = QueryLabData.codePrefix("q37");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    HintedQueries hintedQueries;

    @Autowired
    PlanInspector planInspector;

    @Test
    void hintsTraceTuneAndSteerAQuery() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q37"));

        boolean readOnly = transaction.execute(status -> {
            List<Product> products = hintedQueries.catalogue(PREFIX);
            return entityManager.unwrap(Session.class).isReadOnly(products.getFirst());
        });
        List<String> normal = hintedQueries.catalogue(PREFIX).stream().map(Product::getName).toList();
        List<String> steered = hintedQueries.catalogueAvoidingSequentialScans(PREFIX).stream().map(Product::getName).toList();

        String catalogueSql = "select p.name from sov_product p where p.sku like ? order by p.name, p.id";
        List<String> chosenPlan = planInspector.explain(catalogueSql, PREFIX + "%");
        List<String> steeredPlan = planInspector.explainPreferringIndexes(catalogueSql, PREFIX + "%");

        System.out.println("=== QUERY HINTS ===");
        System.out.println("Catalogue                 : " + normal);
        System.out.println("Same answer when steered  : " + steered.equals(normal));
        System.out.println("Entities loaded read-only : " + readOnly);
        print("Plan PostgreSQL chooses", chosenPlan);
        print("Plan with sequential scans discouraged", steeredPlan);

        assertThat(readOnly).isTrue();
        assertThat(normal).containsExactly(
            "Bread Board", "Cast Iron Pan", "Copper Kettle", "Kitchen Scale", "Loose Leaf Tea", "Stoneware Mug");
        assertThat(steered).isEqualTo(normal);
        assertThat(String.join("\n", steeredPlan)).contains("sov_product_by_name_id");
    }

    private static void print(String title, List<String> plan) {
        System.out.println("=== " + title + " ===");
        plan.forEach(line -> System.out.println("  " + line));
    }

}
