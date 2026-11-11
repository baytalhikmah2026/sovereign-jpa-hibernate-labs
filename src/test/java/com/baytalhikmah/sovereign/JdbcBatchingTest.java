package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.service.CatalogueImport;
import com.baytalhikmah.sovereign.service.CatalogueImport.Trips;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JdbcBatchingTest {

    static final int ROWS = 500;

    @Autowired
    CatalogueImport catalogueImport;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void batchesTurnHundredsOfTripsIntoTen() {
        long start = System.nanoTime();
        Trips insertOne = catalogueImport.insertInBatches("Q53-ONE-", ROWS, 1);
        long insertOneMillis = millisSince(start);
        start = System.nanoTime();
        Trips insertFifty = catalogueImport.insertInBatches("Q53-FIFTY-", ROWS, 50);
        long insertFiftyMillis = millisSince(start);

        Money newPrice = Money.of("10.49", "GBP");
        start = System.nanoTime();
        Trips updateOne = catalogueImport.repriceInBatches("Q53-ONE-", newPrice, 1);
        long updateOneMillis = millisSince(start);
        start = System.nanoTime();
        Trips updateFifty = catalogueImport.repriceInBatches("Q53-FIFTY-", newPrice, 50);
        long updateFiftyMillis = millisSince(start);

        Integer repriced = jdbc.queryForObject(
            "select count(*) from sov_product where sku like 'Q53-%' and price_amount = 10.49", Integer.class);

        System.out.println("=== JDBC BATCHING ===");
        print("Insert, batch size 1 ", insertOne, insertOneMillis);
        print("Insert, batch size 50", insertFifty, insertFiftyMillis);
        print("Update, batch size 1 ", updateOne, updateOneMillis);
        print("Update, batch size 50", updateFifty, updateFiftyMillis);
        System.out.println("Rows with the new price : " + repriced);

        assertThat(insertOne).isEqualTo(new Trips(ROWS, ROWS, 0));
        assertThat(insertFifty).isEqualTo(new Trips(ROWS, 0, 10));
        assertThat(updateOne).isEqualTo(new Trips(ROWS, ROWS + 1, 0));
        assertThat(updateFifty).isEqualTo(new Trips(ROWS, 1, 10));
        assertThat(repriced).isEqualTo(2 * ROWS);
    }

    private static void print(String label, Trips trips, long millis) {
        System.out.println(label + " : " + trips.rows() + " rows, " + trips.statements() + " single statements, "
            + trips.batches() + " batches, " + millis + " ms");
    }

    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

}
