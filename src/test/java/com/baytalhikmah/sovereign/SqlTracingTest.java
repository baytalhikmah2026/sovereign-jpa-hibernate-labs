package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.service.OrderDesk;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.persistence.EntityManager;

@Import({ TestcontainersConfiguration.class, SqlTracingTest.InMemorySpans.class })
@ActiveProfiles("tracing")
@SpringBootTest
class SqlTracingTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class InMemorySpans {

        @Bean
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }

    }

    @Autowired
    OrderDesk orderDesk;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    Tracer sovereignTracer;

    @Autowired
    SdkTracerProvider sovereignTracerProvider;

    @Autowired
    InMemorySpanExporter spans;

    @Test
    void everySqlStatementBecomesAChildSpan() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new Customer("Yara Mansour", "yara@tracing.test"));
            entityManager.persist(new Product("Q82-KETTLE", "Traced Kettle", Money.of("31.00", "EUR")));
        });
        sovereignTracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        spans.reset();

        Span request = sovereignTracer.spanBuilder("place order Q82-0001").startSpan();
        try (Scope current = request.makeCurrent()) {
            orderDesk.placeOrder("Q82-0001", "yara@tracing.test", "Q82-KETTLE", 1);
        }
        finally {
            request.end();
        }
        sovereignTracerProvider.forceFlush().join(10, TimeUnit.SECONDS);

        List<SpanData> finished = spans.getFinishedSpanItems();
        String traceId = request.getSpanContext().getTraceId();
        String requestSpanId = request.getSpanContext().getSpanId();
        List<SpanData> sqlSpans = finished.stream()
            .filter(span -> span.getParentSpanId().equals(requestSpanId))
            .toList();

        System.out.println("=== SQL TRACING ===");
        System.out.println("trace id   : " + traceId);
        System.out.println("root span  : " + request.getSpanContext().getSpanId() + " place order Q82-0001");
        sqlSpans.forEach(span -> System.out.printf("  child    : %s %-26s %6d µs%n", span.getSpanId(),
            span.getName(), (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1_000));
        System.out.println("same trace : " + finished.stream().allMatch(span -> span.getTraceId().equals(traceId)));

        assertThat(sqlSpans).extracting(SpanData::getName).containsSubsequence("SELECT sov_customer",
            "SELECT sov_product", "INSERT sov_purchase_order", "INSERT sov_order_line", "INSERT sov_outbox_event");
        assertThat(finished).allMatch(span -> span.getTraceId().equals(traceId));
        assertThat(sqlSpans).allMatch(span -> span.getAttributes().asMap().values().stream()
            .noneMatch(value -> value.toString().contains("yara@tracing.test")));
    }

}
