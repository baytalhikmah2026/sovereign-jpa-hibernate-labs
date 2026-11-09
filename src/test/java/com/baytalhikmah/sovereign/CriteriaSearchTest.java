package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.ProductCard;
import com.baytalhikmah.sovereign.query.ProductSearch;
import com.baytalhikmah.sovereign.query.ProductSearchFilter;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CriteriaSearchTest {

    static final String PREFIX = QueryLabData.codePrefix("q27");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    ProductSearch productSearch;

    @Test
    void oneSearchMethodServesEveryCombinationOfFilters() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q27"));

        List<ProductCard> all = show("Everything",
            new ProductSearchFilter(PREFIX, null, null, null));
        List<ProductCard> kettles = show("Name contains 'KETTLE'",
            new ProductSearchFilter(PREFIX, "KETTLE", null, null));
        List<ProductCard> midRange = show("Price 15.00 to 40.00",
            new ProductSearchFilter(PREFIX, null, new BigDecimal("15.00"), new BigDecimal("40.00")));

        assertThat(all).extracting(ProductCard::sku)
            .containsExactly("Q27-TEA", "Q27-MUG", "Q27-SCL", "Q27-BRD", "Q27-KET", "Q27-PAN");
        assertThat(kettles).extracting(ProductCard::name).containsExactly("Copper Kettle");
        assertThat(midRange).extracting(ProductCard::sku).containsExactly("Q27-SCL", "Q27-BRD", "Q27-KET");
    }

    private List<ProductCard> show(String title, ProductSearchFilter filter) {
        List<ProductCard> cards = productSearch.search(filter);
        System.out.println("=== " + title + " ===");
        cards.forEach(card -> System.out.println(
            String.format("%-8s %-15s %6s", card.sku(), card.name(), card.price())));
        return cards;
    }

}
