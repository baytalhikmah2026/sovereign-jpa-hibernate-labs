package com.baytalhikmah.sovereign.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** The product showcase page: each product with its category links and its reviews. */
@Service
public class ProductShowcase {

    @PersistenceContext
    private EntityManager entityManager;

    /** Tries to fetch both lists in one query. Hibernate refuses: two bags at once. */
    @Transactional(readOnly = true)
    public List<Product> inOneQuery(String skuPrefix) {
        return entityManager.createQuery("""
                select p from Product p
                left join fetch p.categoryLinks
                left join fetch p.reviews
                where p.sku like :prefix
                order by p.sku""", Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
    }

    /** Fetches one list per query. The second query fills the products the first query already loaded. */
    @Transactional(readOnly = true)
    public List<Product> inTwoQueries(String skuPrefix) {
        List<Product> products = entityManager.createQuery("""
                select p from Product p
                left join fetch p.categoryLinks
                where p.sku like :prefix
                order by p.sku""", Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
        entityManager.createQuery("""
                select p from Product p
                left join fetch p.reviews
                where p in :products""", Product.class)
            .setParameter("products", products)
            .getResultList();
        return products;
    }

}
