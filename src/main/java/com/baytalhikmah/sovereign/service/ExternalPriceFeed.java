package com.baytalhikmah.sovereign.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.baytalhikmah.sovereign.persistence.CacheInvalidationListener;

/** Plays the role of a separate pricing system: it writes prices with plain SQL and announces each change on Kafka. */
@Service
public class ExternalPriceFeed {

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ExternalPriceFeed(JdbcTemplate jdbcTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Changes the price directly in PostgreSQL, behind Hibernate's back. Returns the product's ID. */
    public UUID updatePrice(String sku, BigDecimal newAmount) {
        UUID productId = jdbcTemplate.queryForObject("select id from sov_product where sku = ?", UUID.class, sku);
        jdbcTemplate.update("update sov_product set price_amount = ? where id = ?", newAmount, productId);
        return productId;
    }

    /** Publishes "this product's price changed" and waits until Kafka has stored the message. */
    public void announcePriceChange(UUID productId, String sku) {
        kafkaTemplate.send(CacheInvalidationListener.PRICE_CHANGES, productId.toString(), sku).join();
    }

}
