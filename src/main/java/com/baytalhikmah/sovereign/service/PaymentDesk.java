package com.baytalhikmah.sovereign.service;

import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.BankTransferPayment;
import com.baytalhikmah.sovereign.domain.CardPayment;
import com.baytalhikmah.sovereign.domain.Payment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Prints one receipt line per payment, whatever kind of payment it is. */
@Service
public class PaymentDesk {

    @PersistenceContext
    private EntityManager entityManager;

    /** Checks the kind of payment on the object as it arrives: a proxy hides the real kind. */
    @Transactional(readOnly = true)
    public String naiveReceipt(UUID paymentId) {
        Payment payment = entityManager.getReference(Payment.class, paymentId);
        if (payment instanceof CardPayment card) {
            return "CARD  | " + card.describe();
        }
        if (payment instanceof BankTransferPayment transfer) {
            return "BANK  | " + transfer.describe();
        }
        return "OTHER | " + payment.describe();
    }

    /** Un-proxies the payment first, then checks its real kind. */
    @Transactional(readOnly = true)
    public String receipt(UUID paymentId) {
        Payment payment = entityManager.getReference(Payment.class, paymentId);
        return receiptLine(Hibernate.unproxy(payment, Payment.class));
    }

    private static String receiptLine(Payment payment) {
        return switch (payment) {
            case CardPayment card -> "CARD  | " + card.describe();
            case BankTransferPayment transfer -> "BANK  | " + transfer.describe();
            default -> "OTHER | " + payment.describe();
        };
    }

}
