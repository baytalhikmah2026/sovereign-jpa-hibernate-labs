package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.domain.Address;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.EmailNotice;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductReview;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.domain.SmsNotice;
import com.baytalhikmah.sovereign.service.CustomerErasure;
import com.baytalhikmah.sovereign.service.ErasureCertificate;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RightToBeForgottenTest {

    static final String EMAIL = "layla.haddad@q90.shop.test";

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CustomerErasure erasure;

    @Test
    void theCustomerDisappearsButTheLegalRecordsStay() {
        UUID[] ids = transaction.execute(status -> {
            Customer layla = new Customer("Layla Haddad", EMAIL);
            Address home = new Address("12 Cedar Lane", "Leeds", "LS1 4AB", "GB");
            layla.moveTo(home, home);
            layla.joinLoyalty("GOLD");
            entityManager.persist(layla);
            Product mug = new Product("Q90-MUG", "Forget-Me-Not Mug", Money.of("9.50", "GBP"));
            entityManager.persist(mug);
            PurchaseOrder order = new PurchaseOrder("Q90-0001", layla);
            order.addLine(mug, 2);
            entityManager.persist(order);
            entityManager.persist(new ProductReview(mug, layla, 5, "Lovely mug, says Layla from Leeds"));
            ProductReview hidden = new ProductReview(mug, layla, 2, "Chipped on arrival");
            entityManager.persist(hidden);
            entityManager.flush();
            entityManager.remove(hidden);
            entityManager.persist(new EmailNotice(layla, "Your order Q90-0001", EMAIL));
            entityManager.persist(new SmsNotice(layla, "Your order is on its way", "+447700900123"));
            return new UUID[] { layla.getId(), mug.getId() };
        });
        UUID laylaId = ids[0];
        UUID mugId = ids[1];
        CurrentActor.runAs(EMAIL, () -> transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, mugId).changePrice(Money.of("9.00", "GBP"))));

        System.out.println("=== ERASURE PLAN ===");
        erasure.plan().forEach(step -> System.out.println(step.entity() + "." + step.attribute() + " -> "
                + step.rule().action()));

        ErasureCertificate certificate = erasure.erase(laylaId);

        System.out.println("=== ERASURE CERTIFICATE ===");
        System.out.println("customer        : " + certificate.customerId());
        System.out.println("erased at       : " + certificate.erasedAt());
        System.out.println("rows deleted    : " + certificate.rowsDeleted());
        System.out.println("orders kept     : " + certificate.ordersKeptAnonymised() + " (anonymised customer)");
        System.out.println("revisions       : " + certificate.revisionsScrubbed() + " scrubbed, timeline kept");
        System.out.println("remaining traces: " + certificate.remainingTraces());
        System.out.println("verified        : " + certificate.verified());
        System.out.println("fingerprint     : " + certificate.fingerprint());

        String customerNow = jdbc.queryForObject(
            "select full_name || ' <' || email || '>' from sov_customer where id = ?", String.class, laylaId);
        Integer orderLines = jdbc.queryForObject("""
                select count(*) from sov_order_line l join sov_purchase_order o on o.id = l.order_id
                where o.order_number = 'Q90-0001'""", Integer.class);
        Integer mugHistory = jdbc.queryForObject(
            "select count(*) from sov_product_aud where id = ?", Integer.class, mugId);
        System.out.println("customer row now: " + customerNow);

        assertThat(certificate.verified()).isTrue();
        assertThat(certificate.rowsDeleted())
            .containsEntry("ProductReview", 2)
            .containsEntry("EmailNotice", 1)
            .containsEntry("SmsNotice", 1);
        assertThat(certificate.ordersKeptAnonymised()).isEqualTo(1);
        assertThat(certificate.revisionsScrubbed()).isEqualTo(1);
        assertThat(customerNow).startsWith("Erased customer <erased-").doesNotContain("Layla");
        assertThat(orderLines).isEqualTo(1);
        assertThat(mugHistory).isEqualTo(2);
    }

}
