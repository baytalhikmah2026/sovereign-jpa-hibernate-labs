package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** One printed line of a receipt: what was bought, how many, and the line total. */
public record ReceiptLine(String productName, int quantity, BigDecimal lineTotal) {
}
