package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CustomerLifecycleService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Customer register(Customer customer) {
        System.out.println("[NEW]      managed? " + entityManager.contains(customer) + ", id = " + customer.getId());
        entityManager.persist(customer);
        System.out.println("[MANAGED]  managed? " + entityManager.contains(customer) + ", id = " + customer.getId());
        return customer;
    }

    @Transactional(readOnly = true)
    public boolean loadTwiceFromSameContext(Long id) {
        Customer first = entityManager.find(Customer.class, id);
        Customer second = entityManager.find(Customer.class, id);
        return first == second;
    }

    @Transactional
    public Customer rename(Customer detachedCustomer, String newName) {
        System.out.println("[DETACHED] managed? " + entityManager.contains(detachedCustomer));
        Customer managed = entityManager.merge(detachedCustomer);
        managed.setFullName(newName);
        System.out.println("[MERGED]   managed? " + entityManager.contains(managed));
        return managed;
    }

    @Transactional
    public void remove(Long id) {
        Customer customer = entityManager.find(Customer.class, id);
        entityManager.remove(customer);
        System.out.println("[REMOVED]  managed? " + entityManager.contains(customer));
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return entityManager.find(Customer.class, id);
    }

}
