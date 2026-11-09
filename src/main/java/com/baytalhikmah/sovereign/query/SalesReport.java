package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class SalesReport {

    @PersistenceContext
    private EntityManager entityManager;

    /** GROUP BY + HAVING: customers with at least minOrders orders, biggest spender first. */
    public List<CustomerSpend> repeatCustomers(String emailDomain, long minOrders) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.CustomerSpend(
                    c.fullName, count(distinct o), sum(l.lineTotalAmount))
                from PurchaseOrder o
                join o.customer c
                join o.lines l
                where c.email like :domain
                group by c.fullName
                having count(distinct o) >= :minOrders
                order by sum(l.lineTotalAmount) desc
                """, CustomerSpend.class)
            .setParameter("domain", "%" + emailDomain)
            .setParameter("minOrders", minOrders)
            .getResultList();
    }

    /** A window over the whole result: every product numbered by revenue. */
    public List<ProductRank> bestSellers(String skuPrefix) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.ProductRank(
                    row_number() over (order by sum(l.lineTotalAmount) desc),
                    p.name, sum(l.quantity), sum(l.lineTotalAmount))
                from OrderLine l
                join l.product p
                where p.sku like :prefix
                group by p.name
                order by sum(l.lineTotalAmount) desc
                """, ProductRank.class)
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
    }

    /** A window per customer (PARTITION BY): each customer's orders numbered from the biggest. */
    public List<OrderRank> biggestOrdersPerCustomer(String emailDomain) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.OrderRank(
                    c.fullName,
                    row_number() over (partition by c.fullName order by sum(l.lineTotalAmount) desc),
                    o.orderNumber, sum(l.lineTotalAmount))
                from PurchaseOrder o
                join o.customer c
                join o.lines l
                where c.email like :domain
                group by c.fullName, o.orderNumber
                order by c.fullName, sum(l.lineTotalAmount) desc
                """, OrderRank.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

}
