package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.CustomerSpend;
import com.baytalhikmah.sovereign.query.OrderRank;
import com.baytalhikmah.sovereign.query.ProductRank;
import com.baytalhikmah.sovereign.query.SalesReport;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AggregationTest {

    static final String DOMAIN = QueryLabData.emailDomain("q30");
    static final String PREFIX = QueryLabData.codePrefix("q30");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    SalesReport salesReport;

    @Test
    void theDatabaseCountsRanksAndPartitions() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q30"));

        List<CustomerSpend> repeat = salesReport.repeatCustomers(DOMAIN, 2);
        List<ProductRank> best = salesReport.bestSellers(PREFIX);
        List<OrderRank> perCustomer = salesReport.biggestOrdersPerCustomer(DOMAIN);

        System.out.println("=== REPEAT CUSTOMERS (2+ orders) ===");
        repeat.forEach(row -> System.out.println(
            String.format("%-11s | %d orders | %s", row.customerName(), row.orders(), row.spent())));
        System.out.println("=== BEST SELLERS ===");
        best.forEach(row -> System.out.println(
            String.format("#%d %-14s | %d units | %s", row.position(), row.productName(), row.unitsSold(), row.revenue())));
        System.out.println("=== ORDERS PER CUSTOMER, BIGGEST FIRST ===");
        perCustomer.forEach(row -> System.out.println(
            String.format("%-11s #%d %s | %s", row.customerName(), row.positionForCustomer(), row.orderNumber(), row.total())));

        assertThat(repeat).extracting(CustomerSpend::customerName).containsExactly("Amina Yusuf", "Bilal Ahmed");
        assertThat(best).extracting(ProductRank::productName)
            .containsExactly("Cast Iron Pan", "Copper Kettle", "Stoneware Mug", "Loose Leaf Tea", "Bread Board");
        assertThat(best).extracting(ProductRank::position).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(perCustomer).extracting(OrderRank::orderNumber)
            .containsExactly("Q30-A3", "Q30-A1", "Q30-A2", "Q30-B1", "Q30-B2", "Q30-Z1");
        assertThat(perCustomer).extracting(OrderRank::positionForCustomer).containsExactly(1L, 2L, 3L, 1L, 2L, 1L);
    }

}
