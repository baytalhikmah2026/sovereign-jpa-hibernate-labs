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

import com.baytalhikmah.sovereign.domain.BankTransferPayment;
import com.baytalhikmah.sovereign.domain.CardPayment;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.FixedAmountPromotion;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Payment;
import com.baytalhikmah.sovereign.domain.PercentagePromotion;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.Promotion;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InheritanceTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void joinedInheritanceGivesEachKindItsOwnStrictTable() {
        UUID orderId = transaction.execute(status -> {
            Customer zara = new Customer("Zara Malik", "zara.payments@sovereign.test");
            Product kettle = new Product("KET-PAY", "Copper Kettle", Money.of("39.50", "GBP"));
            entityManager.persist(zara);
            entityManager.persist(kettle);
            PurchaseOrder order = new PurchaseOrder("SO-2001", zara);
            order.addLine(kettle, 2);
            entityManager.persist(order);

            entityManager.persist(new CardPayment(order, Money.of("50.00", "GBP"), "VISA", "4242"));
            entityManager.persist(new BankTransferPayment(order, Money.of("29.00", "GBP"), "GB33BUKB20201555555555", "SO-2001-REST"));
            return order.getId();
        });

        List<String> payments = entityManager.createQuery(
                "select p from Payment p where p.order.id = :orderId", Payment.class)
            .setParameter("orderId", orderId)
            .getResultList()
            .stream().map(Payment::describe).sorted().toList();

        System.out.println("=== JOINED: PAYMENTS ===");
        payments.forEach(line -> System.out.println("  " + line));
        System.out.println("card_brand nullable : " + nullable("sov_card_payment", "card_brand"));
        System.out.println("iban nullable       : " + nullable("sov_bank_transfer_payment", "iban"));

        assertThat(payments).containsExactly(
            "Bank transfer SO-2001-REST: 29.00 GBP", "VISA card ending 4242: 50.00 GBP");
        assertThat(nullable("sov_card_payment", "card_brand")).isEqualTo("NO");
        assertThat(nullable("sov_bank_transfer_payment", "iban")).isEqualTo("NO");
    }

    @Test
    void singleTableKeepsEveryKindInOneTable() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new PercentagePromotion("WELCOME10", 10));
            entityManager.persist(new FixedAmountPromotion("FIVEOFF", Money.of("5.00", "GBP")));
        });

        Money orderTotal = Money.of("91.00", "GBP");
        List<Promotion> promotions = entityManager.createQuery(
                "select p from Promotion p order by p.code", Promotion.class)
            .getResultList();
        List<String> kinds = jdbc.queryForList("select kind from sov_promotion order by code", String.class);

        System.out.println("=== SINGLE TABLE: PROMOTIONS ===");
        promotions.forEach(p -> System.out.println("  " + p.getCode() + " on " + orderTotal + " → " + p.discountFor(orderTotal)));
        System.out.println("kind column          : " + kinds);
        System.out.println("percent_off nullable : " + nullable("sov_promotion", "percent_off"));

        assertThat(promotions).hasSize(2);
        assertThat(promotions.get(0).discountFor(orderTotal)).isEqualTo(Money.of("5.00", "GBP"));
        assertThat(promotions.get(1).discountFor(orderTotal)).isEqualTo(Money.of("9.10", "GBP"));
        assertThat(kinds).containsExactly("FIXED", "PERCENT");
        assertThat(nullable("sov_promotion", "percent_off")).isEqualTo("YES");
    }

    private String nullable(String table, String column) {
        return jdbc.queryForObject(
            "select is_nullable from information_schema.columns where table_name = ? and column_name = ?",
            String.class, table, column);
    }

}
