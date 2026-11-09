package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Customer_;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.domain.PurchaseOrder_;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Repository
public class SubqueryQueries {

    @PersistenceContext
    private EntityManager entityManager;

    /** Uncorrelated: the average is calculated once, then every product is compared with it. */
    public List<String> pricedAboveAverage(String skuPrefix) {
        return entityManager.createQuery("""
                select p.name
                from Product p
                where p.sku like :prefix
                  and p.price.amount > (select avg(p2.price.amount) from Product p2 where p2.sku like :prefix)
                order by p.price.amount
                """, String.class)
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
    }

    /** Correlated: the subquery refers to the outer customer, so it is checked for each customer. */
    public List<String> customersByOrders(String emailDomain, boolean withOrders) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<Customer> customer = query.from(Customer.class);

        Subquery<Integer> anyOrder = query.subquery(Integer.class);
        Root<PurchaseOrder> order = anyOrder.from(PurchaseOrder.class);
        anyOrder.select(cb.literal(1))
            .where(cb.equal(order.get(PurchaseOrder_.customer), customer));

        Predicate hasOrders = cb.exists(anyOrder);
        query.select(customer.get(Customer_.fullName))
            .where(cb.like(customer.get(Customer_.email), "%" + emailDomain),
                   withOrders ? hasOrders : cb.not(hasOrders))
            .orderBy(cb.asc(customer.get(Customer_.fullName)));

        return entityManager.createQuery(query).getResultList();
    }

    /** The same question as customersByOrders(domain, false), asked with NOT IN. */
    public List<String> customersWithoutOrdersUsingNotIn(String emailDomain) {
        return entityManager.createQuery("""
                select c.fullName
                from Customer c
                where c.email like :domain
                  and c.id not in (select o.customer.id from PurchaseOrder o)
                order by c.fullName
                """, String.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

}
