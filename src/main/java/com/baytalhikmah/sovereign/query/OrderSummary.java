package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

import com.baytalhikmah.sovereign.domain.OrderStatus;

/** One line of the "my orders" screen: exactly the five values the screen shows, nothing more. */
public record OrderSummary(
        String orderNumber,
        String customerName,
        OrderStatus status,
        Long lineCount,
        BigDecimal total) {
}
