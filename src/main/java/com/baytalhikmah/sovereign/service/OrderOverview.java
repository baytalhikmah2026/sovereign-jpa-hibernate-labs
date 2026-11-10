package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.OrderLine;
import com.baytalhikmah.sovereign.domain.OrderLine_;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;
import com.baytalhikmah.sovereign.domain.PurchaseOrder_;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Subgraph;

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

    /** The same overview again: the query only filters, the named graph says what to load. */
    @Transactional(readOnly = true)
    public List<String> graphOverview(String emailDomain) {
        return entityManager.createQuery("""
                select o from PurchaseOrder o
                where o.customer.email like :domain
                order by o.orderNumber""", PurchaseOrder.class)
            .setParameter("domain", "%" + emailDomain)
            .setHint(SpecHints.HINT_SPEC_FETCH_GRAPH, entityManager.getEntityGraph(PurchaseOrder.OVERVIEW_GRAPH))
            .getResultList()
            .stream().map(OrderOverview::describe).toList();
    }

    /** One order by its id, loaded with the same named graph. */
    @Transactional(readOnly = true)
    public PurchaseOrder findForOverview(UUID orderId) {
        return entityManager.find(PurchaseOrder.class, orderId,
            Map.of(SpecHints.HINT_SPEC_FETCH_GRAPH, entityManager.getEntityGraph(PurchaseOrder.OVERVIEW_GRAPH)));
    }

    /** List screen: each order with its customer, nothing more. */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> ordersForListScreen(String emailDomain) {
        EntityGraph<PurchaseOrder> graph = entityManager.createEntityGraph(PurchaseOrder.class);
        graph.addAttributeNode(PurchaseOrder_.customer);
        return ordersWithGraph(emailDomain, graph);
    }

    /** Detail screen: each order with its customer, its lines and the product of every line. */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> ordersForDetailScreen(String emailDomain) {
        EntityGraph<PurchaseOrder> graph = entityManager.createEntityGraph(PurchaseOrder.class);
        graph.addAttributeNode(PurchaseOrder_.customer);
        Subgraph<OrderLine> lines = graph.addElementSubgraph(PurchaseOrder_.lines);
        lines.addAttributeNode(OrderLine_.product);
        return ordersWithGraph(emailDomain, graph);
    }

    private List<PurchaseOrder> ordersWithGraph(String emailDomain, EntityGraph<PurchaseOrder> graph) {
        return entityManager.createQuery("""
                select o from PurchaseOrder o
                where o.customer.email like :domain
                order by o.orderNumber""", PurchaseOrder.class)
            .setParameter("domain", "%" + emailDomain)
            .setHint(SpecHints.HINT_SPEC_FETCH_GRAPH, graph)
            .getResultList();
    }

    static String describe(PurchaseOrder order) {
        return order.getOrderNumber() + " | " + order.getCustomer().getFullName()
            + " | " + order.getLines().size() + " lines";
    }

}
