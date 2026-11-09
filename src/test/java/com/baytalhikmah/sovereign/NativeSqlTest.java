package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.NativeSalesQueries;
import com.baytalhikmah.sovereign.query.ProductSales;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NativeSqlTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    NativeSalesQueries nativeSales;

    @Test
    void nativeSqlRowsBecomeRecordsThroughTheMapping() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q32"));

        List<ProductSales> sales = nativeSales.productSales(QueryLabData.codePrefix("q32"));

        System.out.println("=== PRODUCT SALES (native SQL) ===");
        sales.forEach(row -> System.out.println(String.format("%-8s %-15s %2d units %7s  orders: %s",
            row.sku(), row.name(), row.units(), row.revenue(), row.orders())));

        assertThat(sales).hasSize(6);
        assertThat(sales.getFirst()).isEqualTo(
            new ProductSales("Q32-PAN", "Cast Iron Pan", 3L, new BigDecimal("162.00"), "Q32-A3,Q32-Z1"));
        assertThat(sales.getLast().sku()).isEqualTo("Q32-SCL");
        assertThat(sales.getLast().units()).isZero();
        assertThat(sales.getLast().orders()).isEqualTo("-");
    }

}
