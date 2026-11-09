package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** One product tile in the search results. */
public record ProductCard(String sku, String name, BigDecimal price) {
}
