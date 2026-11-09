package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JoinQueries {

    @PersistenceContext
    private EntityManager entityManager;

    /** Every customer paired with every product: rows = customers x products. */
    public long crossJoinCount(String emailDomain, String skuPrefix) {
        return entityManager.createQuery("""
                select count(*)
                from Customer c, Product p
                where c.email like :domain and p.sku like :prefix
                """, Long.class)
            .setParameter("domain", "%" + emailDomain)
            .setParameter("prefix", skuPrefix + "%")
            .getSingleResult();
    }

    /** Only customers who have orders: one row per order. */
    public List<CustomerOrderRow> customersWithOrders(String emailDomain) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.CustomerOrderRow(c.fullName, o.orderNumber)
                from PurchaseOrder o
                join o.customer c
                where c.email like :domain
                order by c.fullName, o.orderNumber
                """, CustomerOrderRow.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

    /** Every customer, with or without orders: customers without orders get one row with a null order. */
    public List<CustomerOrderRow> allCustomersAndTheirOrders(String emailDomain) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.CustomerOrderRow(c.fullName, o.orderNumber)
                from Customer c
                left join PurchaseOrder o on o.customer = c
                where c.email like :domain
                order by c.fullName, o.orderNumber
                """, CustomerOrderRow.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

}
