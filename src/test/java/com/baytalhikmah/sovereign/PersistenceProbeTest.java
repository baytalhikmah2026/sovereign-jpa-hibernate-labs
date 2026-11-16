package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.config.PersistenceHealthIndicator;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PersistenceProbeTest {

    @Autowired
    PersistenceHealthIndicator persistence;

    @Test
    void theProbeAsksTheDatabaseThroughAnEntityManager() {
        Health health = persistence.health();

        System.out.println("=== PERSISTENCE PROBE ===");
        System.out.println("status  : " + health.getStatus());
        System.out.println("details : " + health.getDetails());

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("answer", 1);
    }

}
