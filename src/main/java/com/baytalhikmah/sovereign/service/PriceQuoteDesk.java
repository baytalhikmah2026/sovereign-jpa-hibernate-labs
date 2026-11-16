package com.baytalhikmah.sovereign.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baytalhikmah.sovereign.persistence.RecordNotFoundException;

/**
 * Answers price questions even during a database outage: every fresh price is remembered, and when the catalogue
 * cannot be read (error or open circuit breaker) the last known price is served, marked as stale, as long as it is
 * younger than the allowed maximum age.
 */
@Service
public class PriceQuoteDesk {

    private record Snapshot(String price, Instant readAt) {
    }

    private final CatalogueReader catalogue;

    private final Duration maxStaleness;

    private final Map<String, Snapshot> lastKnown = new ConcurrentHashMap<>();

    @Autowired
    public PriceQuoteDesk(CatalogueReader catalogue,
            @Value("${sovereign.catalogue.max-staleness:10m}") Duration maxStaleness) {
        this.catalogue = catalogue;
        this.maxStaleness = maxStaleness;
    }

    public PriceQuote quoteOf(String sku) {
        try {
            String price = catalogue.priceOf(sku);
            lastKnown.put(sku, new Snapshot(price, Instant.now()));
            return new PriceQuote(sku, price, false, Duration.ZERO);
        }
        catch (RecordNotFoundException missing) {
            throw missing;
        }
        catch (RuntimeException unavailable) {
            Snapshot snapshot = lastKnown.get(sku);
            if (snapshot == null) {
                throw new CatalogueUnavailableException(sku, unavailable);
            }
            Duration age = Duration.between(snapshot.readAt(), Instant.now());
            if (age.compareTo(maxStaleness) > 0) {
                throw new CatalogueUnavailableException(sku, unavailable);
            }
            return new PriceQuote(sku, snapshot.price(), true, age);
        }
    }

}
