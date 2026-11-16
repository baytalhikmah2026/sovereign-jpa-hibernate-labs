package com.baytalhikmah.sovereign.persistence;

import java.util.Optional;
import java.util.UUID;

import com.baytalhikmah.sovereign.domain.Product;

/** Products, found by their SKU. */
public interface ProductRepository extends SovereignRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

}
