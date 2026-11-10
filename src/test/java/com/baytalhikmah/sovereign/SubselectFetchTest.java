package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.ProductCategory;
import com.baytalhikmah.sovereign.service.CategoryMenu;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SubselectFetchTest {

    static final String KEY = "q47-";

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    CategoryMenu categoryMenu;

    @Test
    void oneStatementPerLevelOfTheTree() {
        transaction.executeWithoutResult(status -> createTree());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        List<String> menu = categoryMenu.menu(KEY);
        long statements = statistics.getPrepareStatementCount();

        System.out.println("=== SUBSELECT FETCH ===");
        menu.forEach(line -> System.out.println("| " + line));
        System.out.println("Categories : " + menu.size());
        System.out.println("Statements : " + statements + " (roots + one per level below)");

        assertThat(menu).containsExactly(
            "Garden",
            "  Garden Tools",
            "Kitchen",
            "  Cookware",
            "    Kettles",
            "    Pans",
            "  Tableware",
            "    Mugs");
        assertThat(statements).isEqualTo(4);
    }

    private void createTree() {
        ProductCategory garden = category("garden", "Garden", null);
        category("tools", "Garden Tools", garden);
        ProductCategory kitchen = category("kitchen", "Kitchen", null);
        ProductCategory cookware = category("cookware", "Cookware", kitchen);
        ProductCategory tableware = category("tableware", "Tableware", kitchen);
        category("kettles", "Kettles", cookware);
        category("pans", "Pans", cookware);
        category("mugs", "Mugs", tableware);
        entityManager.flush();
        entityManager.clear();
    }

    private ProductCategory category(String slug, String name, ProductCategory parent) {
        ProductCategory category = new ProductCategory(KEY + slug, name);
        category.placeUnder(parent);
        entityManager.persist(category);
        return category;
    }

}
