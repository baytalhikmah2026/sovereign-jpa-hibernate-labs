package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.JdbcTripCounter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/** Loads a supplier's product list into the catalogue, the classic way and the stateless way. */
@Service
public class CatalogueImport {

    static final int BATCH_SIZE = 50;

    /** How many single statements and how many batches a job sent to the database. */
    public record Trips(int rows, int statements, int batches) {
    }

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

    /** Inserts products in JDBC batches of the given size, flushing and clearing after every batch. */
    @Transactional
    public Trips insertInBatches(String skuPrefix, int count, int batchSize) {
        Session session = entityManager.unwrap(Session.class);
        session.setJdbcBatchSize(batchSize);
        JdbcTripCounter trips = new JdbcTripCounter();
        session.addEventListeners(trips);
        for (int i = 1; i <= count; i++) {
            entityManager.persist(supplierProduct(skuPrefix, i));
            if (i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return new Trips(count, trips.statements(), trips.batches());
    }

    /** Gives every product of one prefix a new price; dirty checking sends the updates in batches. */
    @Transactional
    public Trips repriceInBatches(String skuPrefix, Money newPrice, int batchSize) {
        Session session = entityManager.unwrap(Session.class);
        session.setJdbcBatchSize(batchSize);
        JdbcTripCounter trips = new JdbcTripCounter();
        session.addEventListeners(trips);
        List<Product> products = entityManager.createQuery(
                "select p from Product p where p.sku like :prefix order by p.sku", Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
        products.forEach(product -> product.changePrice(newPrice));
        entityManager.flush();
        return new Trips(products.size(), trips.statements(), trips.batches());
    }

    private static Product supplierProduct(String skuPrefix, int number) {
        return new Product(skuPrefix + String.format(Locale.ROOT, "%05d", number),
            "Supplier item " + number, Money.of("9.99", "GBP"));
    }

}
