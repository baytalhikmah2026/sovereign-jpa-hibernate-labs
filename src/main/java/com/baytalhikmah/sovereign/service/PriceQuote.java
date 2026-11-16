package com.baytalhikmah.sovereign.service;

import java.time.Duration;

/** A price answer. stale = true means the last known price, served while the database could not be reached. */
public record PriceQuote(String sku, String price, boolean stale, Duration age) {
}
