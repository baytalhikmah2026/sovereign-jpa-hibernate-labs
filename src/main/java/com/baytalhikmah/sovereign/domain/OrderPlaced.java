package com.baytalhikmah.sovereign.domain;

import java.math.BigDecimal;

/** The content of the "OrderPlaced" message: what other systems need to know about a new order. */
public record OrderPlaced(String orderNumber, String customerEmail, int lines, BigDecimal total, String currency) {
}
