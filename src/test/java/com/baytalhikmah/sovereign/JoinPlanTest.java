package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.CustomerOrderRow;
import com.baytalhikmah.sovereign.query.JoinQueries;
import com.baytalhikmah.sovereign.query.PlanInspector;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JoinPlanTest {

    static final String DOMAIN = QueryLabData.emailDomain("q29");
    static final String PREFIX = QueryLabData.codePrefix("q29");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JoinQueries joinQueries;

    @Autowired
    PlanInspector planInspector;

    @Test
    void threeJoinsThreeAnswersAndThePlansBehindThem() {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q29"));

        long cross = joinQueries.crossJoinCount(DOMAIN, PREFIX);
        List<CustomerOrderRow> inner = joinQueries.customersWithOrders(DOMAIN);
        List<CustomerOrderRow> left = joinQueries.allCustomersAndTheirOrders(DOMAIN);

        System.out.println("=== THREE JOINS ===");
        System.out.println("Cross join rows : " + cross);
        System.out.println("Inner join rows : " + inner.size());
        System.out.println("Left join rows  : " + left.size());
        left.forEach(row -> System.out.println("  " + row.customerName() + " -> " + row.orderNumber()));

        UUID aminaId = jdbc.queryForObject("select id from sov_customer where email = ?", UUID.class, "amina" + DOMAIN);
        String ordersOfOneCustomer = "select o.order_number from sov_purchase_order o where o.customer_id = ?";
        List<String> crossPlan = planInspector.explain(
            "select count(*) from sov_customer c cross join sov_product p");
        List<String> chosenPlan = planInspector.explain(ordersOfOneCustomer, aminaId);
        List<String> indexPlan = planInspector.explainPreferringIndexes(ordersOfOneCustomer, aminaId);

        print("Cross join plan", crossPlan);
        print("Orders of one customer, plan PostgreSQL chooses", chosenPlan);
        print("Orders of one customer, plan with sequential scans discouraged", indexPlan);

        assertThat(cross).isEqualTo(24);
        assertThat(inner).hasSize(6).noneMatch(row -> row.customerName().equals("Omar Farouk"));
        assertThat(left).hasSize(7).contains(new CustomerOrderRow("Omar Farouk", null));
        assertThat(String.join("\n", crossPlan)).contains("Nested Loop");
        assertThat(String.join("\n", indexPlan)).contains("sov_purchase_order_by_customer");
    }

    private static void print(String title, List<String> plan) {
        System.out.println("=== " + title + " ===");
        plan.forEach(line -> System.out.println("  " + line));
    }

}
