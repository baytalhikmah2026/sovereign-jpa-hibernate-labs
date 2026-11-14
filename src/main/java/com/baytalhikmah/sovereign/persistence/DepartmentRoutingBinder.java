package com.baytalhikmah.sovereign.persistence;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

import com.baytalhikmah.sovereign.domain.Product;

/**
 * Sends each product's search document to a shard chosen from its department (the SKU part before the
 * first dash, for example "KIT" for KIT-Q76-KETTLE). All products of one department share one shard.
 */
public class DepartmentRoutingBinder implements RoutingBinder {

    @Override
    public void bind(RoutingBindingContext context) {
        context.dependencies().use("sku");
        context.bridge(Product.class, new DepartmentRoute());
    }

    private static final class DepartmentRoute implements RoutingBridge<Product> {

        @Override
        public void route(DocumentRoutes routes, Object entityIdentifier, Product product,
                RoutingBridgeRouteContext context) {
            routes.addRoute().routingKey(product.getDepartment());
        }

        /** A SKU never changes, so a product's document is always found on the shard of its department. */
        @Override
        public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Product product,
                RoutingBridgeRouteContext context) {
            routes.addRoute().routingKey(product.getDepartment());
        }

    }

}
