package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.baytalhikmah.sovereign.config.HibernateMetricsConfig;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.persistence.CustomerRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("metrics")
@SpringBootTest
class HibernateMetricsTest {

    @Autowired
    CustomerRepository customers;

    @Autowired
    MeterRegistry registry;

    @Autowired
    PrometheusMeterRegistry prometheus;

    @Test
    void hibernateCountersBecomeMetrics() {
        double insertsBefore = counter("hibernate.entities.inserts");
        double queriesBefore = counter("hibernate.query.executions");

        customers.saveAll(List.of(
            new Customer("Nadia Karim", "nadia@metrics.test", "web"),
            new Customer("Tariq Aziz", "tariq@metrics.test", "web"),
            new Customer("Huda Saleh", "huda@metrics.test", "agent:metrics-bot")));
        int webCustomers = customers.findBySignupChannelOrderByFullName("web").size();

        double inserts = counter("hibernate.entities.inserts") - insertsBefore;
        double queries = counter("hibernate.query.executions") - queriesBefore;

        System.out.println("=== HIBERNATE METRICS ===");
        System.out.println("web customers found : " + webCustomers);
        System.out.println("entity inserts      : +" + (long) inserts);
        System.out.println("query executions    : +" + (long) queries);
        prometheus.scrape().lines()
            .filter(line -> line.startsWith("hibernate_entities_inserts_total")
                || line.startsWith("hibernate_query_executions_total")
                || line.startsWith("hibernate_sessions_open_total")
                || line.startsWith("hibernate_transactions_total"))
            .sorted()
            .forEach(System.out::println);

        assertThat(inserts).isEqualTo(3.0);
        assertThat(queries).isEqualTo(1.0);
        assertThat(prometheus.scrape()).contains("hibernate_entities_inserts_total{entityManagerFactory=\"sovereign\"}");
    }

    private double counter(String name) {
        return registry.get(name).tag("entityManagerFactory", HibernateMetricsConfig.FACTORY_NAME).functionCounter()
            .count();
    }

}
