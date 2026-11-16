package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Service;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.persistence.ProductRepository;
import com.baytalhikmah.sovereign.persistence.RecordNotFoundException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Reads catalogue prices through the circuit breaker "catalogue-db". When the database keeps failing, the breaker
 * opens and further calls fail at once, instead of each one waiting for the connection pool to give up.
 */
@Service
public class CatalogueReader {

    public static final String BREAKER = "catalogue-db";

    private final ProductRepository products;

    private final CircuitBreaker breaker;

    public CatalogueReader(ProductRepository products, CircuitBreakerRegistry breakers) {
        this.products = products;
        this.breaker = breakers.circuitBreaker(BREAKER);
        this.breaker.getEventPublisher().onStateTransition(event -> System.out.println(
            "circuit " + BREAKER + " : " + event.getStateTransition().getFromState() + " -> "
                + event.getStateTransition().getToState()));
    }

    /** The product's price as "27.00 EUR". */
    public String priceOf(String sku) {
        return breaker.executeSupplier(() -> {
            Money price = products.findBySku(sku).orElseThrow(() -> new RecordNotFoundException("Product", sku))
                .getPrice();
            return price.amount() + " " + price.currency();
        });
    }

    public CircuitBreaker.State state() {
        return breaker.getState();
    }

}
