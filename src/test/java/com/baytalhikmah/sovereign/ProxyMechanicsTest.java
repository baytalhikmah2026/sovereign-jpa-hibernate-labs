package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.BankTransferPayment;
import com.baytalhikmah.sovereign.domain.CardPayment;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Payment;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.service.PaymentDesk;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ProxyMechanicsTest {

    static final String KEY = "q50";

    record Inspection(String proxyClass, boolean initializedAtFirst, long statementsAtFirst,
            boolean instanceOfCard, String realClass, boolean initializedAfter, long statementsAfter,
            boolean unproxiedIsCard, boolean sameObject) {
    }

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    PaymentDesk paymentDesk;

    @Test
    void aProxyHidesTheRealKindUntilItIsUnproxied() {
        List<UUID> ids = transaction.execute(status -> createPayments());
        UUID cardId = ids.get(0);
        UUID transferId = ids.get(1);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        Inspection inspection = transaction.execute(status -> {
            statistics.clear();
            Payment payment = entityManager.getReference(Payment.class, cardId);
            boolean initializedAtFirst = Hibernate.isInitialized(payment);
            long statementsAtFirst = statistics.getPrepareStatementCount();
            String proxyClass = payment.getClass().getSimpleName();
            boolean instanceOfCard = payment instanceof CardPayment;
            String realClass = Hibernate.getClass(payment).getSimpleName();
            Object real = Hibernate.unproxy(payment);
            return new Inspection(proxyClass, initializedAtFirst, statementsAtFirst, instanceOfCard, realClass,
                Hibernate.isInitialized(payment), statistics.getPrepareStatementCount(),
                real instanceof CardPayment, real == payment);
        });

        System.out.println("=== PROXY MECHANICS ===");
        System.out.println("getReference returns     : " + inspection.proxyClass());
        System.out.println("Loaded at first          : " + inspection.initializedAtFirst()
            + " (" + inspection.statementsAtFirst() + " statements)");
        System.out.println("instanceof CardPayment   : " + inspection.instanceOfCard());
        System.out.println("Real class               : " + inspection.realClass()
            + " (loaded=" + inspection.initializedAfter() + ", " + inspection.statementsAfter() + " statement)");
        System.out.println("Unproxied is a card      : " + inspection.unproxiedIsCard()
            + " (same object as the proxy: " + inspection.sameObject() + ")");
        System.out.println("--- receipts ---");
        System.out.println("Naive  : " + paymentDesk.naiveReceipt(cardId));
        System.out.println("Naive  : " + paymentDesk.naiveReceipt(transferId));
        System.out.println("Safe   : " + paymentDesk.receipt(cardId));
        System.out.println("Safe   : " + paymentDesk.receipt(transferId));

        assertThat(inspection.proxyClass()).startsWith("Payment$HibernateProxy");
        assertThat(inspection.initializedAtFirst()).isFalse();
        assertThat(inspection.statementsAtFirst()).isZero();
        assertThat(inspection.instanceOfCard()).isFalse();
        assertThat(inspection.realClass()).isEqualTo("CardPayment");
        assertThat(inspection.initializedAfter()).isTrue();
        assertThat(inspection.statementsAfter()).isEqualTo(1);
        assertThat(inspection.unproxiedIsCard()).isTrue();
        assertThat(inspection.sameObject()).isFalse();
        assertThat(paymentDesk.naiveReceipt(cardId)).startsWith("OTHER | VISA card ending 4242");
        assertThat(paymentDesk.receipt(cardId)).isEqualTo("CARD  | VISA card ending 4242: 50.00 GBP");
        assertThat(paymentDesk.receipt(transferId)).isEqualTo("BANK  | Bank transfer Q50-A1-REST: 29.00 GBP");
    }

    private List<UUID> createPayments() {
        QueryLabData.create(entityManager, KEY);
        PurchaseOrder order = entityManager.createQuery(
                "select o from PurchaseOrder o where o.orderNumber = :number", PurchaseOrder.class)
            .setParameter("number", QueryLabData.codePrefix(KEY) + "A1")
            .getSingleResult();
        CardPayment card = new CardPayment(order, Money.of("50.00", "GBP"), "VISA", "4242");
        BankTransferPayment transfer = new BankTransferPayment(order, Money.of("29.00", "GBP"),
            "GB33BUKB20201555555555", "Q50-A1-REST");
        entityManager.persist(card);
        entityManager.persist(transfer);
        return List.of(card.getId(), transfer.getId());
    }

}
