package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;
import java.util.UUID;

/** One line of the catalogue list. The id is kept so the next page can start right after it. */
public record CatalogueEntry(UUID id, String sku, String name, BigDecimal price) {
}
