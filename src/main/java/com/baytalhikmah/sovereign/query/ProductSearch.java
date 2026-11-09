package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Repository;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class ProductSearch {

    @PersistenceContext
    private EntityManager entityManager;

    public List<ProductCard> search(ProductSearchFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Product> product = query.from(Product.class);
        Path<String> sku = product.<String>get("sku");
        Path<String> name = product.<String>get("name");
        Path<BigDecimal> price = product.<Money>get("price").<BigDecimal>get("amount");

        List<Predicate> where = new ArrayList<>();
        if (filter.skuPrefix() != null) {
            where.add(cb.like(sku, filter.skuPrefix() + "%"));
        }
        if (filter.nameContains() != null) {
            where.add(cb.like(cb.lower(name), "%" + filter.nameContains().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filter.minPrice() != null) {
            where.add(cb.greaterThanOrEqualTo(price, filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            where.add(cb.lessThanOrEqualTo(price, filter.maxPrice()));
        }

        query.multiselect(sku.alias("sku"), name.alias("name"), price.alias("price"))
            .where(where.toArray(Predicate[]::new))
            .orderBy(cb.asc(price), cb.asc(sku));

        return entityManager.createQuery(query).getResultList().stream()
            .map(row -> new ProductCard(
                row.get("sku", String.class),
                row.get("name", String.class),
                row.get("price", BigDecimal.class)))
            .toList();
    }

}
