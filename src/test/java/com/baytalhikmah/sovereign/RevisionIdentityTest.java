package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.audit.RequestOrigin;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RevisionIdentityTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyRevisionKnowsWhoWhereAndWhichTrace() {
        UUID id = transaction.execute(status -> {
            Product clock = new Product("Q88-CLOCK", "Wall Clock", Money.of("30.00", "EUR"));
            entityManager.persist(clock);
            return clock.getId();
        });

        String traceId;
        try (SdkTracerProvider tracing = SdkTracerProvider.builder().build()) {
            Span span = tracing.get("lab-88").spanBuilder("change price Q88-CLOCK").startSpan();
            traceId = span.getSpanContext().getTraceId();
            try (Scope current = span.makeCurrent(); RequestOrigin.Scope origin = RequestOrigin.enter("203.0.113.7")) {
                CurrentActor.runAs("amina.pricing", () -> transaction.executeWithoutResult(status ->
                    entityManager.find(Product.class, id).changePrice(Money.of("32.50", "EUR"))));
            }
            finally {
                span.end();
            }
        }

        List<Map<String, Object>> trail = jdbc.queryForList("""
                select a.revtype, a.price_amount, r.username, r.client_ip, r.trace_id
                from sov_product_aud a
                join sov_audit_revision r on r.id = a.rev
                where a.id = ?
                order by a.rev""", id);

        System.out.println("=== AUDIT TRAIL Q88-CLOCK ===");
        trail.forEach(row -> System.out.println("type " + row.get("revtype") + " | " + row.get("price_amount")
                + " | by " + row.get("username") + " | from " + row.get("client_ip") + " | trace " + row.get("trace_id")));

        assertThat(trail).hasSize(2);
        assertThat(trail.get(0)).containsEntry("username", "system").containsEntry("client_ip", null);
        assertThat(trail.get(1)).containsEntry("username", "amina.pricing")
            .containsEntry("client_ip", "203.0.113.7")
            .containsEntry("trace_id", traceId);
    }

}
