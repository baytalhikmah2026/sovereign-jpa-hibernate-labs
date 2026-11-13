package com.baytalhikmah.sovereign.domain;

/** Something happened to the stock of one item: the new quantity on hand and the new version number. */
public record StockChanged(String warehouseCode, String sku, int quantityOnHand, long version) {
}
