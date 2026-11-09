package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.ProductCategory;
import com.baytalhikmah.sovereign.query.CategoryTree;
import com.baytalhikmah.sovereign.query.CategoryTreeRow;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecursiveCteTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    CategoryTree categoryTree;

    @Test
    void oneStatementWalksTheWholeCategoryTree() {
        transaction.executeWithoutResult(status -> {
            ProductCategory kitchen = category("q39-kitchen", "Kitchen", null);
            ProductCategory cookware = category("q39-cookware", "Cookware", kitchen);
            ProductCategory tableware = category("q39-tableware", "Tableware", kitchen);
            category("q39-pans", "Pans", cookware);
            category("q39-kettles", "Kettles", cookware);
            category("q39-mugs", "Mugs", tableware);
            ProductCategory garden = category("q39-garden", "Garden", null);
            category("q39-tools", "Tools", garden);
        });

        List<CategoryTreeRow> rows = transaction.execute(status -> categoryTree.subtree("q39-kitchen"));

        System.out.println("=== CATEGORY TREE UNDER q39-kitchen ===");
        rows.forEach(row -> System.out.println("  ".repeat(row.getDepth() + 1) + row.getSlug() + " (depth " + row.getDepth() + ")"));

        assertThat(rows).extracting(CategoryTreeRow::getSlug).containsExactly(
            "q39-kitchen", "q39-cookware", "q39-tableware", "q39-kettles", "q39-mugs", "q39-pans");
        assertThat(rows).extracting(CategoryTreeRow::getDepth).containsExactly(0, 1, 1, 2, 2, 2);
    }

    private ProductCategory category(String slug, String name, ProductCategory parent) {
        ProductCategory category = new ProductCategory(slug, name);
        if (parent != null) {
            category.placeUnder(parent);
        }
        entityManager.persist(category);
        return category;
    }

}
