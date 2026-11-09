package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class NativeSalesQueries {

    private static final String PRODUCT_SALES = """
            select p.sku,
                   p.name,
                   coalesce(sum(l.quantity), 0) as units,
                   coalesce(sum(l.line_total_amount), 0.00) as revenue,
                   coalesce(string_agg(distinct o.order_number, ',' order by o.order_number), '-') as orders
            from sov_product p
            left join sov_order_line l on l.product_id = p.id
            left join sov_purchase_order o on o.id = l.order_id
            where p.sku like :prefix
            group by p.sku, p.name
            order by revenue desc, p.sku
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ProductSales> productSales(String skuPrefix) {
        return entityManager.createNativeQuery(PRODUCT_SALES, "ProductSales")
            .setParameter("prefix", skuPrefix + "%")
            .getResultList();
    }

}
