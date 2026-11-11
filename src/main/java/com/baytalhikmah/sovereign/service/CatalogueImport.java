package com.baytalhikmah.sovereign.service;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/** Loads a supplier's product list into the catalogue, the classic way and the stateless way. */
@Service
public class CatalogueImport {

    static final int BATCH_SIZE = 50;

    @PersistenceContext
    private EntityManager entityManager;

    private final SessionFactory sessionFactory;

    public CatalogueImport(EntityManagerFactory entityManagerFactory) {
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    /** Classic way: every new product stays managed until the transaction ends. Returns the managed count. */
    @Transactional
    public int importManaged(String skuPrefix, int count) {
        for (int i = 1; i <= count; i++) {
            entityManager.persist(supplierProduct(skuPrefix, i));
        }
        entityManager.flush();
        return entityManager.unwrap(Session.class).getStatistics().getEntityCount();
    }

    /** Stateless way: each product goes straight into a JDBC batch and is forgotten. Returns the rows sent. */
    public int importStateless(String skuPrefix, int count) {
        return sessionFactory.fromStatelessTransaction(session -> {
            session.setJdbcBatchSize(BATCH_SIZE);
            for (int i = 1; i <= count; i++) {
                session.insert(supplierProduct(skuPrefix, i));
            }
            return count;
        });
    }

    /** Reads the same product twice in one stateless session: there is no cache to remember the first read. */
    public boolean sameObjectOnSecondRead(UUID productId) {
        return sessionFactory.fromStatelessSession(session ->
            session.get(Product.class, productId) == session.get(Product.class, productId));
    }

    private static Product supplierProduct(String skuPrefix, int number) {
        return new Product(skuPrefix + String.format(Locale.ROOT, "%05d", number),
            "Supplier item " + number, Money.of("9.99", "GBP"));
    }

}
