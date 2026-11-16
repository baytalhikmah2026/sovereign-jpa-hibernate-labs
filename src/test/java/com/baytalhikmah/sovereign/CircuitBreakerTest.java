package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.ProductRepository;
import com.baytalhikmah.sovereign.service.CatalogueReader;

import eu.rekawek.toxiproxy.Proxy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@Import(ChaosTestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class CircuitBreakerTest {

    @Autowired
    CatalogueReader catalogue;

    @Autowired
    ProductRepository products;

    @Autowired
    Proxy postgresProxy;

    @Test
    void theBreakerOpensDuringAnOutageAndClosesAfterIt() throws Exception {
        products.save(new Product("Q83-KETTLE", "Breaker Kettle", Money.of("27.00", "EUR")));
        System.out.println("=== CIRCUIT BREAKER ===");
        String normal = catalogue.priceOf("Q83-KETTLE");
        System.out.println("normal call  : " + normal + " (state " + catalogue.state() + ")");

        postgresProxy.disable();
        List<String> outage = new ArrayList<>();
        for (int call = 1; call <= 4; call++) {
            long start = System.nanoTime();
            try {
                catalogue.priceOf("Q83-KETTLE");
                outage.add("answered");
            }
            catch (RuntimeException failed) {
                long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
                outage.add("failed");
                System.out.println("outage call " + call + " : failed after " + millis + " ms ("
                    + failed.getClass().getSimpleName() + ")");
            }
        }
        CircuitBreaker.State duringOutage = catalogue.state();

        long start = System.nanoTime();
        Throwable fastFailure = null;
        try {
            catalogue.priceOf("Q83-KETTLE");
        }
        catch (CallNotPermittedException rejected) {
            fastFailure = rejected;
        }
        long fastMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        System.out.println("call 5       : " + (fastFailure == null ? "answered" : "rejected") + " after "
            + fastMillis + " ms (state " + duringOutage + ")");

        postgresProxy.enable();
        Thread.sleep(2500);
        String trial1 = catalogue.priceOf("Q83-KETTLE");
        String trial2 = catalogue.priceOf("Q83-KETTLE");
        System.out.println("trial calls  : " + trial1 + ", " + trial2 + " (state " + catalogue.state() + ")");

        assertThat(normal).isEqualTo("27.00 EUR");
        assertThat(outage).containsOnly("failed");
        assertThat(duringOutage).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(fastFailure).isInstanceOf(CallNotPermittedException.class);
        assertThat(fastMillis).isLessThan(200);
        assertThat(trial1).isEqualTo("27.00 EUR");
        assertThat(trial2).isEqualTo("27.00 EUR");
        assertThat(catalogue.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

}
