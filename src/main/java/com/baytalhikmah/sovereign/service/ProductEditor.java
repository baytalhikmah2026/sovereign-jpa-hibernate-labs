package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.JdbcTripCounter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Saves product names edited on a back-office screen: once with merge(), once with fetch-and-modify. */
@Service
public class ProductEditor {

    /** Products saved, single statements and batches sent to the database. */
    public record SaveReport(int products, int statements, int batches) {
    }

    @PersistenceContext
    private EntityManager entityManager;

    /** Loads the products for the edit screen. They are detached as soon as this method returns. */
    @Transactional(readOnly = true)
    public List<Product> loadForEditing(String skuPrefix) {
        return entityManager.createQuery(
                "select p from Product p where p.sku like :prefix order by p.sku", Product.class)
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
    }

    /** The antipattern: hand every edited, detached product to merge(). */
    @Transactional
    public SaveReport saveWithMerge(List<Product> editedProducts) {
        JdbcTripCounter trips = countTrips();
        for (Product edited : editedProducts) {
            entityManager.merge(edited);
        }
        entityManager.flush();
        return new SaveReport(editedProducts.size(), trips.statements(), trips.batches());
    }

    /** Fetch-and-modify: load the managed products in one query and change only what was edited. */
    @Transactional
    public SaveReport saveWithFetchAndModify(Map<UUID, String> newNames) {
        JdbcTripCounter trips = countTrips();
        List<Product> products = entityManager.createQuery(
                "select p from Product p where p.id in :ids", Product.class)
            .setParameter("ids", newNames.keySet())
            .getResultList();
        for (Product product : products) {
            product.rename(newNames.get(product.getId()));
        }
        entityManager.flush();
        return new SaveReport(products.size(), trips.statements(), trips.batches());
    }

    private JdbcTripCounter countTrips() {
        JdbcTripCounter trips = new JdbcTripCounter();
        entityManager.unwrap(Session.class).addEventListeners(trips);
        return trips;
    }

}
