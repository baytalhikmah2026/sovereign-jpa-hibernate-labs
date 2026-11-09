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
        return orders.stream()
            .map(order -> order.getOrderNumber() + " | " + order.getCustomer().getFullName()
                + " | " + order.getLines().size() + " lines")
            .toList();
    }

}
