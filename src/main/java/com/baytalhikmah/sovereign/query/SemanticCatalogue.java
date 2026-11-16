package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.ProductRepository;
import com.baytalhikmah.sovereign.persistence.RecordNotFoundException;

/**
 * The semantic search an AI shopping agent calls: "what matches this meaning?" and "what is like this product?".
 * The embeddings come from an embedding model; this service only stores and compares them.
 */
@Service
@Transactional(readOnly = true)
public class SemanticCatalogue {

    private final ProductRepository products;

    public SemanticCatalogue(ProductRepository products) {
        this.products = products;
    }

    public List<SimilarProduct> search(String skuPrefix, float[] meaning, int limit) {
        return products.findSimilar(skuPrefix, meaning, Limit.of(limit));
    }

    /** Products of the same department closest in meaning to the given one, without the product itself. */
    public List<SimilarProduct> moreLike(String sku, int limit) {
        Product product = products.findBySku(sku).orElseThrow(() -> new RecordNotFoundException("Product", sku));
        if (product.getEmbedding() == null) {
            return List.of();
        }
        return products.findSimilar(product.getDepartment() + "-", product.getEmbedding(), Limit.of(limit + 1))
            .stream()
            .filter(hit -> !hit.sku().equals(sku))
            .limit(limit)
            .toList();
    }

}
