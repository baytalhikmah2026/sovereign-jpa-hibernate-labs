package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
import com.baytalhikmah.sovereign.service.CatalogueUnavailableException;
import com.baytalhikmah.sovereign.service.PriceQuote;
import com.baytalhikmah.sovereign.service.PriceQuoteDesk;

import eu.rekawek.toxiproxy.Proxy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@Import(ChaosTestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class StaleFallbackTest {

    @Autowired
    PriceQuoteDesk quotes;

    @Autowired
    CatalogueReader catalogue;

    @Autowired
    ProductRepository products;

    @Autowired
    Proxy postgresProxy;

    @Test
    void customersSeeTheLastKnownPriceDuringAnOutage() throws Exception {
        products.save(new Product("Q84-TEA", "Fallback Tea", Money.of("12.50", "EUR")));
        PriceQuoteDesk strictDesk = new PriceQuoteDesk(catalogue, Duration.ofMillis(1));

        System.out.println("=== STALE FALLBACK ===");
        PriceQuote fresh = quotes.quoteOf("Q84-TEA");
        strictDesk.quoteOf("Q84-TEA");
        System.out.println("before outage : " + describe(fresh));

        postgresProxy.disable();
        List<PriceQuote> duringOutage = new ArrayList<>();
        for (int call = 1; call <= 5; call++) {
            PriceQuote quote = quotes.quoteOf("Q84-TEA");
            duringOutage.add(quote);
            System.out.println("outage call " + call + " : " + describe(quote) + " (breaker " + catalogue.state() + ")");
        }
        Throwable tooOld = catchThrowable(() -> strictDesk.quoteOf("Q84-TEA"));
        Throwable neverSeen = catchThrowable(() -> quotes.quoteOf("Q84-COFFEE"));
        System.out.println("max age 1 ms  : " + tooOld.getMessage());
        System.out.println("never read    : " + neverSeen.getMessage());

        postgresProxy.enable();
        Thread.sleep(2500);
        PriceQuote back1 = quotes.quoteOf("Q84-TEA");
        PriceQuote back2 = quotes.quoteOf("Q84-TEA");
        System.out.println("after outage  : " + describe(back2) + " (breaker " + catalogue.state() + ")");

        assertThat(fresh.stale()).isFalse();
        assertThat(duringOutage).allMatch(quote -> quote.stale() && quote.price().equals("12.50 EUR"));
        assertThat(tooOld).isInstanceOf(CatalogueUnavailableException.class);
        assertThat(neverSeen).isInstanceOf(CatalogueUnavailableException.class);
        assertThat(back1.stale()).isFalse();
        assertThat(back2.stale()).isFalse();
        assertThat(catalogue.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private static String describe(PriceQuote quote) {
        return quote.price() + (quote.stale() ? " STALE, " + quote.age().toSeconds() + " s old" : " fresh");
    }

}
