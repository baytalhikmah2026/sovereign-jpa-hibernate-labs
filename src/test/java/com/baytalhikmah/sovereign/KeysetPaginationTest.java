package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.query.CatalogueEntry;
import com.baytalhikmah.sovereign.query.CataloguePager;
import com.baytalhikmah.sovereign.query.PlanInspector;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class KeysetPaginationTest {

    static final String PREFIX = "Q34-";
    static final List<String> NAMES = List.of(
        "Apron", "Bowl", "Cup", "Dish", "Egg Timer", "Fork", "Glass", "Hob Cleaner", "Ice Tray", "Jug");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    CataloguePager pager;

    @Autowired
    PlanInspector planInspector;

    @Test
    void keysetPagesMatchOffsetPagesWithoutSkippingRows() {
        transaction.executeWithoutResult(status -> {
            for (int i = 1; i <= 30; i++) {
                String name = NAMES.get((i - 1) % NAMES.size());
                entityManager.persist(new Product(PREFIX + String.format("%02d", i), name, Money.of("5.00", "GBP")));
            }
        });

        List<List<CatalogueEntry>> offsetPages = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            offsetPages.add(pager.offsetPage(PREFIX, page, 10));
        }

        List<List<CatalogueEntry>> keysetPages = new ArrayList<>();
        List<CatalogueEntry> page = pager.firstPage(PREFIX, 10);
        while (!page.isEmpty()) {
            keysetPages.add(page);
            page = pager.pageAfter(PREFIX, page.getLast(), 10);
        }

        System.out.println("=== CATALOGUE, 10 PER PAGE (keyset) ===");
        for (int i = 0; i < keysetPages.size(); i++) {
            System.out.println("Page " + (i + 1) + ": " + keysetPages.get(i).stream()
                .map(entry -> entry.name() + " " + entry.sku().substring(PREFIX.length())).toList());
        }
        System.out.println("Offset pages equal keyset pages: " + offsetPages.equals(keysetPages));

        List<String> plan = planInspector.explainPreferringIndexes("""
            select p.id from sov_product p
            where (p.name, p.id) > (?, ?)
            order by p.name, p.id
            limit 10""", "Dish", UUID.fromString("00000000-0000-0000-0000-000000000000"));
        System.out.println("=== Plan of a keyset page ===");
        plan.forEach(line -> System.out.println("  " + line));

        assertThat(keysetPages).hasSize(3);
        assertThat(keysetPages).isEqualTo(offsetPages);
        assertThat(keysetPages.getFirst().getFirst().name()).isEqualTo("Apron");
        assertThat(keysetPages.getLast().getLast().name()).isEqualTo("Jug");
        assertThat(String.join("\n", plan)).contains("sov_product_by_name_id");
    }

}
