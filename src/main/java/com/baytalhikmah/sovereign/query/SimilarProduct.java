package com.baytalhikmah.sovereign.query;

/** One semantic search hit: the product and how far its meaning is from the question (0 = identical). */
public record SimilarProduct(String sku, String name, Double distance) {
}
