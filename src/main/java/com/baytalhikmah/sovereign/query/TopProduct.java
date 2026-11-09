package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** One row of the database function sov_top_products. */
public record TopProduct(String sku, Long units, BigDecimal revenue) {
}
