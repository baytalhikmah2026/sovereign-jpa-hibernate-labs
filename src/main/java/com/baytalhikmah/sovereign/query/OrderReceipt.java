package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;
import java.util.List;

/** A whole receipt as plain data: no entity, no proxy, no session needed to read it. */
public record OrderReceipt(String orderNumber, String customerName, List<ReceiptLine> lines) {

    public BigDecimal total() {
        return lines.stream().map(ReceiptLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
