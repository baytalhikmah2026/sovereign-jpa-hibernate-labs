package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class OrderQueries {

    @PersistenceContext
    private EntityManager entityManager;

    public List<OrderSummary> summariesFor(String emailDomain) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.OrderSummary(
                    o.orderNumber, c.fullName, o.status, count(l), sum(l.lineTotalAmount))
                from PurchaseOrder o
                join o.customer c
                join o.lines l
                where c.email like :domain
                group by o.orderNumber, c.fullName, o.status
                order by o.orderNumber
                """, OrderSummary.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

}
