package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** What a shopper typed into the search form. Every field is optional: null means "no filter". */
public record ProductSearchFilter(
        String skuPrefix,
        String nameContains,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}
