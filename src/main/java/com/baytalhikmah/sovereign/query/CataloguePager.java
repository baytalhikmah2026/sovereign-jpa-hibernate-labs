package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class CataloguePager {

    private static final String SELECT = """
            select new com.baytalhikmah.sovereign.query.CatalogueEntry(p.id, p.sku, p.name, p.price.amount)
            from Product p
            """;

    @PersistenceContext
    private EntityManager entityManager;

    /** Classic paging: skip pageNumber * pageSize rows, then take pageSize. */
    public List<CatalogueEntry> offsetPage(String skuPrefix, int pageNumber, int pageSize) {
        return entityManager.createQuery(SELECT + """
                where p.sku like :prefix
                order by p.name, p.id
                """, CatalogueEntry.class)
            .setParameter("prefix", skuPrefix + "%")
            .setFirstResult(pageNumber * pageSize)
            .setMaxResults(pageSize)
            .getResultList();
    }

    /** Keyset paging, first page: nothing to start after yet. */
    public List<CatalogueEntry> firstPage(String skuPrefix, int pageSize) {
        return entityManager.createQuery(SELECT + """
                where p.sku like :prefix
                order by p.name, p.id
                """, CatalogueEntry.class)
            .setParameter("prefix", skuPrefix + "%")
            .setMaxResults(pageSize)
            .getResultList();
    }

    /** Keyset paging, next page: seek straight to the rows after the last entry of the previous page. */
    public List<CatalogueEntry> pageAfter(String skuPrefix, CatalogueEntry last, int pageSize) {
        return entityManager.createQuery(SELECT + """
                where p.sku like :prefix
                  and (p.name, p.id) > (:lastName, :lastId)
                order by p.name, p.id
                """, CatalogueEntry.class)
            .setParameter("prefix", skuPrefix + "%")
            .setParameter("lastName", last.name())
            .setParameter("lastId", last.id())
            .setMaxResults(pageSize)
            .getResultList();
    }

}
