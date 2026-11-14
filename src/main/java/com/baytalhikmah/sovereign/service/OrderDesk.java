package com.baytalhikmah.sovereign.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.OrderPlaced;
import com.baytalhikmah.sovereign.domain.OutboxEvent;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.json.JsonMapper;

/** Takes orders. Each order and its "OrderPlaced" message are saved together, or not at all. */
@Service
public class OrderDesk {

    public static final String ORDER_AGGREGATE = "order";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public UUID placeOrder(String orderNumber, String customerEmail, String sku, int quantity) {
        Customer customer = entityManager
            .createQuery("select c from Customer c where c.email = :email", Customer.class)
            .setParameter("email", customerEmail)
            .getSingleResult();
        Product product = entityManager
            .createQuery("select p from Product p where p.sku = :sku", Product.class)
            .setParameter("sku", sku)
            .getSingleResult();

        PurchaseOrder order = new PurchaseOrder(orderNumber, customer);
        order.addLine(product, quantity);
        entityManager.persist(order);

        Money total = order.total();
        OrderPlaced event = new OrderPlaced(orderNumber, customerEmail, order.getLines().size(), total.amount(),
            total.currency());
        entityManager.persist(new OutboxEvent(ORDER_AGGREGATE, orderNumber, "OrderPlaced",
            JSON.writeValueAsString(event)));

        entityManager.flush();
        return order.getId();
    }

}
