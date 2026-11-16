package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.audit.ProductHistory;
import com.baytalhikmah.sovereign.audit.ProductVersion;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TemporalQueryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    ProductHistory history;

    @Test
    void theHistoryAnswersWhatItLookedLikeAtAnyMoment() throws Exception {
        UUID id = transaction.execute(status -> {
            Product kettle = new Product("Q89-KETTLE", "Kettle", Money.of("20.00", "EUR"));
            entityManager.persist(kettle);
            return kettle.getId();
        });
        Instant afterCreate = Instant.now();
        Thread.sleep(20);
        CurrentActor.runAs("bilal.pricing", () -> transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, id).changePrice(Money.of("22.00", "EUR"))));
        Instant afterPriceChange = Instant.now();
        Thread.sleep(20);
        CurrentActor.runAs("zara.catalogue", () -> transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, id).rename("Copper Kettle")));

        List<ProductVersion> timeline = history.timeline(id);
        Product atCreate = history.asOf(id, afterCreate).orElseThrow();
        Product atPriceChange = history.asOf(id, afterPriceChange).orElseThrow();
        Product beforeEverything = history.asOf(id, Instant.parse("2000-01-01T00:00:00Z")).orElse(null);
        Product today = transaction.execute(status -> entityManager.find(Product.class, id));

        System.out.println("=== TIMELINE Q89-KETTLE ===");
        timeline.forEach(version -> System.out.println("rev " + version.revision() + " | " + version.action()
                + " | by " + version.changedBy() + " | " + version.name() + " | " + version.price()
                + " | changed " + version.changedFields()));
        System.out.println("as of after create       : " + atCreate.getName() + ", " + atCreate.getPrice());
        System.out.println("as of after price change : " + atPriceChange.getName() + ", " + atPriceChange.getPrice());
        System.out.println("as of 1 January 2000     : " + beforeEverything);
        System.out.println("current product          : " + today.getName() + ", " + today.getPrice());

        assertThat(timeline).extracting(ProductVersion::action).containsExactly("ADD", "MOD", "MOD");
        assertThat(timeline.get(1).changedFields()).containsExactly("price");
        assertThat(timeline.get(2).changedFields()).containsExactly("name");
        assertThat(timeline.get(2).changedBy()).isEqualTo("zara.catalogue");
        assertThat(atCreate.getPrice()).isEqualTo(Money.of("20.00", "EUR"));
        assertThat(atPriceChange.getName()).isEqualTo("Kettle");
        assertThat(atPriceChange.getPrice()).isEqualTo(Money.of("22.00", "EUR"));
        assertThat(beforeEverything).isNull();
        assertThat(today.getName()).isEqualTo("Copper Kettle");
    }

}
