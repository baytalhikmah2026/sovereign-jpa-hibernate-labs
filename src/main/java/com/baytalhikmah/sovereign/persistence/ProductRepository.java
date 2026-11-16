package com.baytalhikmah.sovereign.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.query.SimilarProduct;

/** Products, found by their SKU or by the meaning of their embedding. */
public interface ProductRepository extends SovereignRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    /**
     * Hybrid search: a normal filter (SKU prefix) and a ranking by meaning. cosine_distance comes from Hibernate's
     * vector module and becomes the pgvector operator &lt;=&gt;, which the HNSW index can serve.
     */
    @Query("""
            select new com.baytalhikmah.sovereign.query.SimilarProduct(p.sku, p.name, cosine_distance(p.embedding, :meaning))
            from Product p
            where p.embedding is not null and p.sku like concat(:skuPrefix, '%')
            order by cosine_distance(p.embedding, :meaning)""")
    List<SimilarProduct> findSimilar(String skuPrefix, float[] meaning, Limit limit);

}
