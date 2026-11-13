package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** The customer list of the current shop. The code never names a shop: Hibernate picks the tenant. */
@Repository
public class ShopCustomers {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public UUID register(String fullName, String email) {
        Customer customer = new Customer(fullName, email);
        entityManager.persist(customer);
        return customer.getId();
    }

    @Transactional(readOnly = true)
    public List<String> names() {
        return entityManager.createQuery("select c.fullName from Customer c order by c.fullName", String.class)
            .getResultList();
    }

    /** The schema the current session works in, as PostgreSQL sees it. */
    @Transactional(readOnly = true)
    public String currentSchema() {
        return (String) entityManager.createNativeQuery("select current_schema()").getSingleResult();
    }

}
