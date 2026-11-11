package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.service.CatalogueImport;
import com.baytalhikmah.sovereign.service.PriceSweep;
import com.baytalhikmah.sovereign.service.PriceSweep.SweepReport;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PersistenceContextMemoryTest {

    static final int ROWS = 20_000;

    @Autowired
    CatalogueImport catalogueImport;

    @Autowired
    PriceSweep priceSweep;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void clearingKeepsThePersistenceContextSmall() {
        catalogueImport.importStateless("Q56-", ROWS);
        Money fiftyPence = Money.of("0.50", "GBP");

        SweepReport keepEverything = priceSweep.raisePrices("Q56-", fiftyPence, 0);
        SweepReport clearEvery500 = priceSweep.raisePrices("Q56-", fiftyPence, 500);

        Integer repriced = jdbc.queryForObject(
            "select count(*) from sov_product where sku like 'Q56-%' and price_amount = 10.99", Integer.class);

        System.out.println("=== PERSISTENCE CONTEXT SWEEP (" + ROWS + " products) ===");
        print("Keep everything   ", keepEverything);
        print("Flush+clear 500   ", clearEvery500);
        System.out.println("Products at 10.99 : " + repriced);

        assertThat(keepEverything.processed()).isEqualTo(ROWS);
        assertThat(keepEverything.peakManaged()).isEqualTo(ROWS);
        assertThat(clearEvery500.processed()).isEqualTo(ROWS);
        assertThat(clearEvery500.peakManaged()).isEqualTo(500);
        assertThat(repriced).isEqualTo(ROWS);
    }

    private static void print(String label, SweepReport report) {
        System.out.println(label + " : " + report.processed() + " processed, peak " + report.peakManaged()
            + " managed, flush " + report.flushMillis() + " ms, heap about " + report.peakHeapMegabytes() + " MB");
    }

}
