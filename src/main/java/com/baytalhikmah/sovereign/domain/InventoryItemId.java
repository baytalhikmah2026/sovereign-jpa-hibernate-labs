package com.baytalhikmah.sovereign.domain;

import java.io.Serializable;

public record InventoryItemId(String warehouseCode, String sku) implements Serializable {
}
