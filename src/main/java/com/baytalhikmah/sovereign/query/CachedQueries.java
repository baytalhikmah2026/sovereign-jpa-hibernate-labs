package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.TaxRate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Queries whose results may be kept in the query cache, each in its own region. */
@Repository
public class CachedQueries {

    public static final String CATALOGUE_QUERIES = "catalogue_queries";
    public static final String REFERENCE_QUERIES = "reference_queries";

    @PersistenceContext
    private EntityManager entityManager;

    /** Products in a price band: the product table changes often, so this result goes stale often. */
    public List<Product> productsInPriceBand(String skuPrefix, BigDecimal min, BigDecimal max) {
        return entityManager.createQuery("""
                select p
                from Product p
                where p.sku like :prefix
                  and p.price.amount between :min and :max
                order by p.sku
                """, Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .setParameter("min", min)
            .setParameter("max", max)
            .setHint(HibernateHints.HINT_CACHEABLE, true)
            .setHint(HibernateHints.HINT_CACHE_REGION, CATALOGUE_QUERIES)
            .getResultList();
    }

    /** Tax rates of one country: the tax table almost never changes, so this result stays valid. */
    public List<TaxRate> taxRatesFor(String countryCode) {
        return entityManager.createQuery("""
                select t
                from TaxRate t
                where t.countryCode = :country
                order by t.code
                """, TaxRate.class)
            .setParameter("country", countryCode)
            .setHint(HibernateHints.HINT_CACHEABLE, true)
            .setHint(HibernateHints.HINT_CACHE_REGION, REFERENCE_QUERIES)
            .getResultList();
    }

}
