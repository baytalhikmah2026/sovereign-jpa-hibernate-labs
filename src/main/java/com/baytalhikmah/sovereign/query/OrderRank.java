package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** One order and its place among the same customer's orders. */
public record OrderRank(String customerName, Long positionForCustomer, String orderNumber, BigDecimal total) {
}
