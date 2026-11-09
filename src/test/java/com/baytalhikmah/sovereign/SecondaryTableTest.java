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

import com.baytalhikmah.sovereign.domain.Customer;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SecondaryTableTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void oneCustomerObjectSpansTwoTables() {
        UUID[] ids = transaction.execute(status -> {
            Customer maryam = new Customer("Maryam Rahman", "maryam.loyalty@sovereign.test");
            maryam.joinLoyalty("GOLD");
            maryam.addLoyaltyPoints(120);
            Customer yusuf = new Customer("Yusuf Aziz", "yusuf.loyalty@sovereign.test");
            entityManager.persist(maryam);
            entityManager.persist(yusuf);
            return new UUID[] { maryam.getId(), yusuf.getId() };
        });

        Customer member = transaction.execute(status -> entityManager.find(Customer.class, ids[0]));
        Customer guest = transaction.execute(status -> entityManager.find(Customer.class, ids[1]));
        List<String> loyaltyRows = jdbc.queryForList(
            "select loyalty_tier || ' ' || loyalty_points from sov_customer_loyalty where customer_id in (?, ?)",
            String.class, ids[0], ids[1]);

        System.out.println("=== SECONDARY TABLE ===");
        System.out.println("Maryam in Java : " + member.getLoyaltyTier() + ", " + member.getLoyaltyPoints() + " points");
        System.out.println("Yusuf in Java  : " + guest.getLoyaltyTier() + ", " + guest.getLoyaltyPoints() + " points");
        System.out.println("Loyalty rows   : " + loyaltyRows);

        assertThat(member.getLoyaltyTier()).isEqualTo("GOLD");
        assertThat(member.getLoyaltyPoints()).isEqualTo(120);
        assertThat(guest.getLoyaltyTier()).isNull();
        assertThat(loyaltyRows).containsExactly("GOLD 120");
    }

}
