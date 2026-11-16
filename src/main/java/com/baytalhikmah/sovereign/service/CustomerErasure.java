package com.baytalhikmah.sovereign.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.CustomerIdentity;
import com.baytalhikmah.sovereign.domain.EmailNotice;
import com.baytalhikmah.sovereign.domain.ProductReview;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.domain.SmsNotice;
import com.baytalhikmah.sovereign.persistence.RecordNotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * GDPR "right to be forgotten". The plan is found by walking the JPA metamodel: every entity that points to
 * Customer needs a rule, otherwise erasure is refused. Rows that hold only the customer's own content are
 * deleted with plain SQL (so soft-delete rules cannot keep them); records the law requires us to keep stay,
 * pointing to the anonymised customer. Audit revisions keep their place in the timeline but lose the person.
 */
@Service
public class CustomerErasure {

    public enum Action { DELETE_ROWS, KEEP_ANONYMISED }

    public record Rule(Action action, String table, String column) {
    }

    public record Step(String entity, String attribute, Rule rule) {
    }

    static final String ERASED_ACTOR = "erased-subject";

    private static final Map<Class<?>, Rule> RULES = Map.of(
            PurchaseOrder.class, new Rule(Action.KEEP_ANONYMISED, "sov_purchase_order", "customer_id"),
            CustomerIdentity.class, new Rule(Action.DELETE_ROWS, "sov_customer_identity", "customer_id"),
            ProductReview.class, new Rule(Action.DELETE_ROWS, "sov_product_review", "customer_id"),
            EmailNotice.class, new Rule(Action.DELETE_ROWS, "sov_email_notice", "customer_id"),
            SmsNotice.class, new Rule(Action.DELETE_ROWS, "sov_sms_notice", "customer_id"));

    private final EntityManager entityManager;

    public CustomerErasure(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Every association to Customer in the model, with its rule. Fails if any association has no rule. */
    public List<Step> plan() {
        List<Step> steps = new ArrayList<>();
        for (EntityType<?> type : entityManager.getMetamodel().getEntities()) {
            for (Attribute<?, ?> attribute : type.getAttributes()) {
                if (attribute instanceof SingularAttribute<?, ?> && attribute.getJavaType() == Customer.class) {
                    Rule rule = RULES.get(type.getJavaType());
                    if (rule == null) {
                        throw new IllegalStateException("No erasure rule for " + type.getName() + "."
                                + attribute.getName() + ": erasure refused until one is defined");
                    }
                    steps.add(new Step(type.getName(), attribute.getName(), rule));
                }
            }
        }
        steps.sort(Comparator.comparing(Step::entity));
        return steps;
    }

    @Transactional
    public ErasureCertificate erase(UUID customerId) {
        Customer customer = entityManager.find(Customer.class, customerId);
        if (customer == null) {
            throw new RecordNotFoundException("Customer", customerId);
        }
        String email = customer.getEmail();
        String name = customer.getFullName();

        Map<String, Integer> rowsDeleted = new TreeMap<>();
        int ordersKept = 0;
        for (Step step : plan()) {
            Rule rule = step.rule();
            if (rule.action() == Action.DELETE_ROWS) {
                int rows = entityManager
                    .createNativeQuery("delete from " + rule.table() + " where " + rule.column() + " = :id")
                    .setParameter("id", customerId)
                    .executeUpdate();
                rowsDeleted.put(step.entity(), rows);
            }
            else {
                ordersKept += count("select count(*) from " + rule.table() + " where " + rule.column() + " = :id",
                        Map.of("id", customerId));
            }
        }
        int revisionsScrubbed = entityManager
            .createNativeQuery("update sov_audit_revision set username = :erased, client_ip = null where username = :email")
            .setParameter("erased", ERASED_ACTOR)
            .setParameter("email", email)
            .executeUpdate();

        customer.anonymise("Erased customer", "erased-" + customerId + "@erased.invalid");
        entityManager.flush();

        long remaining = count("""
                select (select count(*) from sov_customer where id = :id and (email = :email or full_name = :name))
                     + (select count(*) from sov_customer_loyalty where customer_id = :id)
                     + (select count(*) from sov_customer_identity where customer_id = :id)
                     + (select count(*) from sov_product_review where customer_id = :id)
                     + (select count(*) from sov_email_notice where customer_id = :id or email_address = :email)
                     + (select count(*) from sov_sms_notice where customer_id = :id)
                     + (select count(*) from sov_audit_revision where username = :email)""",
                Map.of("id", customerId, "email", email, "name", name));

        Instant erasedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String fingerprint = sha256(customerId + "|" + erasedAt + "|" + rowsDeleted + "|" + ordersKept + "|"
                + revisionsScrubbed + "|" + remaining);
        return new ErasureCertificate(customerId, erasedAt, rowsDeleted, ordersKept, revisionsScrubbed, remaining,
                fingerprint);
    }

    private int count(String sql, Map<String, Object> parameters) {
        var query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).intValue();
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

}
