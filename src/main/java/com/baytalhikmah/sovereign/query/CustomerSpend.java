package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** One customer and how much they spent. */
public record CustomerSpend(String customerName, Long orders, BigDecimal spent) {
}
