package com.baytalhikmah.sovereign.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** The "my orders" overview: order number, customer name and number of lines for each order. */
@Service
public class OrderOverview {

    @PersistenceContext
    private EntityManager entityManager;

    /** Loads the orders, then lets lazy loading fetch each customer and each line list on first use. */
    @Transactional(readOnly = true)
    public List<String> naiveOverview(String emailDomain) {
        List<PurchaseOrder> orders = entityManager.createQuery("""
                select o from PurchaseOrder o
                join o.customer c
                where c.email like :domain
                order by o.orderNumber""", PurchaseOrder.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
        return orders.stream().map(OrderOverview::describe).toList();
    }

    /** The same overview from one query: customers and lines arrive together with the orders. */
    @Transactional(readOnly = true)
    public List<String> joinFetchOverview(String emailDomain) {
        return ordersWithCustomerAndLines(emailDomain).stream().map(OrderOverview::describe).toList();
    }

    /** Orders with their customer and lines already loaded, so they can be used after the transaction. */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> ordersWithCustomerAndLines(String emailDomain) {
        return entityManager.createQuery("""
                select o from PurchaseOrder o
                join fetch o.customer c
                join fetch o.lines
                where c.email like :domain
                order by o.orderNumber""", PurchaseOrder.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

    static String describe(PurchaseOrder order) {
        return order.getOrderNumber() + " | " + order.getCustomer().getFullName()
            + " | " + order.getLines().size() + " lines";
    }

}
