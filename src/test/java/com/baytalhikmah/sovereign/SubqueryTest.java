package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.PlanInspector;
import com.baytalhikmah.sovereign.query.SubqueryQueries;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SubqueryTest {

    static final String DOMAIN = QueryLabData.emailDomain("q31");
    static final String PREFIX = QueryLabData.codePrefix("q31");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    SubqueryQueries subqueries;

    @Autowired
    PlanInspector planInspector;

    @Test
    void subqueriesAnswerOnceOrOncePerRow() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q31"));

        List<String> aboveAverage = subqueries.pricedAboveAverage(PREFIX);
        List<String> buyers = subqueries.customersByOrders(DOMAIN, true);
        List<String> notYetBuyers = subqueries.customersByOrders(DOMAIN, false);
        List<String> notYetBuyersNotIn = subqueries.customersWithoutOrdersUsingNotIn(DOMAIN);

        System.out.println("=== SUBQUERIES ===");
        System.out.println("Priced above the average : " + aboveAverage);
        System.out.println("Customers with orders    : " + buyers);
        System.out.println("Customers without orders : " + notYetBuyers + " (NOT EXISTS)");
        System.out.println("Customers without orders : " + notYetBuyersNotIn + " (NOT IN)");

        List<String> antiJoinPlan = planInspector.explain("""
            select c.full_name from sov_customer c
            where not exists (select 1 from sov_purchase_order o where o.customer_id = c.id)""");
        List<String> perRowPlan = planInspector.explain("""
            select c.full_name,
                   (select count(*) from sov_purchase_order o where o.customer_id = c.id) as orders
            from sov_customer c""");
        print("NOT EXISTS plan", antiJoinPlan);
        print("Subquery in the select list plan", perRowPlan);

        assertThat(aboveAverage).containsExactly("Copper Kettle", "Cast Iron Pan");
        assertThat(buyers).containsExactly("Amina Yusuf", "Bilal Ahmed", "Zara Khan");
        assertThat(notYetBuyers).containsExactly("Omar Farouk");
        assertThat(notYetBuyersNotIn).isEqualTo(notYetBuyers);
        assertThat(String.join("\n", antiJoinPlan)).contains("Anti Join");
        assertThat(String.join("\n", perRowPlan)).contains("SubPlan");
    }

    private static void print(String title, List<String> plan) {
        System.out.println("=== " + title + " ===");
        plan.forEach(line -> System.out.println("  " + line));
    }

}
