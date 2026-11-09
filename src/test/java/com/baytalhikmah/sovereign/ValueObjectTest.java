package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Address;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ValueObjectTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void addressesAreStoredInsideTheCustomerRow() {
        Address home = new Address("12 Market Street", "Leeds", "LS1 6AB", "gb");
        Address office = new Address("4 Dock Road", "Manchester", "M3 4LZ", "GB");

        UUID id = transaction.execute(status -> {
            Customer amina = new Customer("Amina Yusuf", "amina.vo@sovereign.test");
            amina.moveTo(home, office);
            entityManager.persist(amina);
            return amina.getId();
        });

        Customer loaded = transaction.execute(status -> entityManager.find(Customer.class, id));
        List<String> addressColumns = jdbc.queryForList("""
            select column_name from information_schema.columns
            where table_name = 'sov_customer' and (column_name like 'billing%' or column_name like 'shipping%')
            order by column_name""", String.class);

        System.out.println("=== VALUE OBJECTS ===");
        System.out.println("Billing  : " + loaded.getBillingAddress());
        System.out.println("Shipping : " + loaded.getShippingAddress());
        System.out.println("Columns  : " + addressColumns);

        assertThat(loaded.getBillingAddress()).isEqualTo(home);
        assertThat(loaded.getBillingAddress().countryCode()).isEqualTo("GB");
        assertThat(addressColumns).hasSize(8);
    }

    @Test
    void moneyProtectsItself() {
        Money price = Money.of("19.9", "GBP");
        Money total = price.times(3).plus(Money.of("5", "GBP"));

        System.out.println("=== MONEY ===");
        System.out.println("Price : " + price);
        System.out.println("Total : " + total);

        assertThat(total).isEqualTo(Money.of("64.70", "GBP"));
        assertThatThrownBy(() -> Money.of("-1", "GBP")).hasMessageContaining("cannot be negative");
        assertThatThrownBy(() -> Money.of("1", "XYZ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> price.plus(Money.of("1", "EUR"))).hasMessageContaining("Cannot add EUR to GBP");
    }

    @Test
    void aProductCarriesItsPriceAsOneValue() {
        UUID id = transaction.execute(status -> {
            Product kettle = new Product("KET-001", "Copper Kettle", Money.of("39.50", "GBP"));
            entityManager.persist(kettle);
            return kettle.getId();
        });

        String row = jdbc.queryForObject(
            "select sku || ' | ' || price_amount || ' | ' || price_currency from sov_product where id = ?",
            String.class, id);
        System.out.println("Product row : " + row);

        assertThat(row).isEqualTo("KET-001 | 39.50 | GBP");
    }

}
