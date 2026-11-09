package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;

/** Sales of one product, including products that never sold. orders lists the order numbers, or "-". */
public record ProductSales(String sku, String name, Long units, BigDecimal revenue, String orders) {
}
