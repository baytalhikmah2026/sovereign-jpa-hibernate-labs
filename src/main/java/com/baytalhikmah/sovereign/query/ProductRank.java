package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** A product's place in the best-seller list. */
public record ProductRank(Long position, String productName, Long unitsSold, BigDecimal revenue) {
}
