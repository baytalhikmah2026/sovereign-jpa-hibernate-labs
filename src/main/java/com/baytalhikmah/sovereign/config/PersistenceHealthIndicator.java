package com.baytalhikmah.sovereign.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Health check named "persistence": opens a real EntityManager and asks the database a tiny question.
 * Kubernetes uses it (through the readiness group) to decide whether the pod may receive traffic.
 */
@Component("persistence")
public class PersistenceHealthIndicator implements HealthIndicator {

    private static final String CHECK = "EntityManager native query: select 1";

    private final EntityManagerFactory entityManagerFactory;

    public PersistenceHealthIndicator(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Health health() {
        long started = System.nanoTime();
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            Object answer = entityManager.createNativeQuery("select 1").getSingleResult();
            long millis = (System.nanoTime() - started) / 1_000_000;
            return Health.up()
                .withDetail("check", CHECK)
                .withDetail("answer", answer)
                .withDetail("millis", millis)
                .build();
        }
        catch (RuntimeException failure) {
            return Health.down(failure).withDetail("check", CHECK).build();
        }
    }

}
