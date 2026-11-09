package com.baytalhikmah.sovereign.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class ServerSideLogic {

    @PersistenceContext
    private EntityManager entityManager;

    /** A scalar function: one value back. */
    public BigDecimal lifetimeValue(UUID customerId) {
        return (BigDecimal) entityManager.createNativeQuery("select sov_customer_lifetime_value(:customerId)", BigDecimal.class)
            .setParameter("customerId", customerId)
            .getSingleResult();
    }

    /** A table-valued function: rows back, used like a table in FROM. */
    @SuppressWarnings("unchecked")
    public List<TopProduct> topProducts(String skuPrefix, int limit) {
        List<Tuple> rows = entityManager.createNativeQuery(
                "select sku, units, revenue from sov_top_products(:prefix, :limit)", Tuple.class)
            .setParameter("prefix", skuPrefix)
            .setParameter("limit", limit)
            .getResultList();
        return rows.stream()
            .map(row -> new TopProduct(
                row.get("sku", String.class),
                row.get("units", Long.class),
                row.get("revenue", BigDecimal.class)))
            .toList();
    }

    /** A stored procedure, declared once with @NamedStoredProcedureQuery on InventoryItem. */
    @Transactional
    public void restock(String warehouseCode, String sku, int quantity) {
        entityManager.createNamedStoredProcedureQuery("InventoryItem.restock")
            .setParameter("p_warehouse_code", warehouseCode)
            .setParameter("p_sku", sku)
            .setParameter("p_quantity", quantity)
            .execute();
    }

}
