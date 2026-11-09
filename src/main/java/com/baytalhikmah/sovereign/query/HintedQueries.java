package com.baytalhikmah.sovereign.query;

import java.sql.Statement;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class HintedQueries {

    /** Appears in front of the SQL, so whoever reads the database logs knows where it came from. */
    public static final String CATALOGUE_COMMENT = "catalogue-screen HintedQueries.catalogue";

    @PersistenceContext
    private EntityManager entityManager;

    /** One query, four hints: a traceable comment, read-only entities, 100 rows per round trip, 2 seconds at most. */
    public List<Product> catalogue(String skuPrefix) {
        return entityManager.createQuery("""
                select p from Product p
                where p.sku like :prefix
                order by p.name, p.id""", Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .setHint(HibernateHints.HINT_COMMENT, CATALOGUE_COMMENT)
            .setHint(HibernateHints.HINT_READ_ONLY, true)
            .setHint(HibernateHints.HINT_FETCH_SIZE, 100)
            .setHint(SpecHints.HINT_SPEC_QUERY_TIMEOUT, 2000)
            .getResultList();
    }

    /** The same query, with PostgreSQL told for this transaction only to avoid sequential scans. */
    @Transactional(readOnly = true)
    public List<Product> catalogueAvoidingSequentialScans(String skuPrefix) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("set local enable_seqscan = off");
            }
        });
        return catalogue(skuPrefix);
    }

}
