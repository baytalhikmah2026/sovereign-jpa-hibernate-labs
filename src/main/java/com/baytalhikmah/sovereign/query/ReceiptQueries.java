package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

/** Builds read-only receipts from one flat query, without loading a single entity. */
@Repository
public class ReceiptQueries {

    @PersistenceContext
    private EntityManager entityManager;

    public List<OrderReceipt> receiptsFor(String emailDomain) {
        List<Tuple> rows = entityManager.createQuery("""
                select o.orderNumber as orderNumber, c.fullName as customerName,
                       p.name as productName, l.quantity as quantity, l.lineTotalAmount as lineTotal
                from PurchaseOrder o
                join o.customer c
                join o.lines l
                join l.product p
                where c.email like :domain
                order by o.orderNumber, p.name""", Tuple.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();

        Map<String, OrderReceipt> receipts = new LinkedHashMap<>();
        for (Tuple row : rows) {
            OrderReceipt receipt = receipts.computeIfAbsent(row.get("orderNumber", String.class),
                number -> new OrderReceipt(number, row.get("customerName", String.class), new ArrayList<>()));
            receipt.lines().add(new ReceiptLine(row.get("productName", String.class),
                row.get("quantity", Integer.class), row.get("lineTotal", BigDecimal.class)));
        }
        return List.copyOf(receipts.values());
    }

}
