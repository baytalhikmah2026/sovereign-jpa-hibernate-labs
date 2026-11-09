package com.baytalhikmah.sovereign;

import java.util.List;
import java.util.Locale;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.OrderStatus;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.PurchaseOrder;

import jakarta.persistence.EntityManager;

/**
 * The same small shop for every query lab. Each lab passes its own key (for example "q26"):
 * emails end in "@q26.shop.test" and SKUs and order numbers start with "Q26-",
 * so the rows of one lab never mix with the rows of another.
 */
final class QueryLabData {

    private QueryLabData() {
    }

    static String emailDomain(String key) {
        return "@" + key + ".shop.test";
    }

    static String codePrefix(String key) {
        return key.toUpperCase(Locale.ROOT) + "-";
    }

    static void create(EntityManager entityManager, String key) {
        String domain = emailDomain(key);
        String prefix = codePrefix(key);

        Customer amina = new Customer("Amina Yusuf", "amina" + domain, "web");
        Customer bilal = new Customer("Bilal Ahmed", "bilal" + domain, "mobile");
        Customer zara = new Customer("Zara Khan", "zara" + domain, "store");
        Customer omar = new Customer("Omar Farouk", "omar" + domain, "agent:shop-assistant");
        List.of(amina, bilal, zara, omar).forEach(entityManager::persist);

        Product kettle = product(entityManager, prefix + "KET", "Copper Kettle", "39.50");
        Product mug = product(entityManager, prefix + "MUG", "Stoneware Mug", "12.00");
        Product tea = product(entityManager, prefix + "TEA", "Loose Leaf Tea", "8.75");
        Product pan = product(entityManager, prefix + "PAN", "Cast Iron Pan", "54.00");
        Product board = product(entityManager, prefix + "BRD", "Bread Board", "22.00");
        product(entityManager, prefix + "SCL", "Kitchen Scale", "18.25");

        PurchaseOrder a1 = order(entityManager, prefix + "A1", amina, OrderStatus.PAID);
        a1.addLine(kettle, 1);
        a1.addLine(mug, 2);
        PurchaseOrder a2 = order(entityManager, prefix + "A2", amina, OrderStatus.DELIVERED);
        a2.addLine(tea, 4);
        PurchaseOrder a3 = order(entityManager, prefix + "A3", amina, OrderStatus.NEW);
        a3.addLine(pan, 1);
        a3.addLine(board, 1);
        PurchaseOrder b1 = order(entityManager, prefix + "B1", bilal, OrderStatus.SHIPPED);
        b1.addLine(mug, 4);
        b1.addLine(tea, 2);
        PurchaseOrder b2 = order(entityManager, prefix + "B2", bilal, OrderStatus.PAID);
        b2.addLine(kettle, 1);
        PurchaseOrder z1 = order(entityManager, prefix + "Z1", zara, OrderStatus.DELIVERED);
        z1.addLine(pan, 2);

        entityManager.flush();
        entityManager.clear();
    }

    private static Product product(EntityManager entityManager, String sku, String name, String price) {
        Product product = new Product(sku, name, Money.of(price, "GBP"));
        entityManager.persist(product);
        return product;
    }

    private static PurchaseOrder order(EntityManager entityManager, String number, Customer customer, OrderStatus status) {
        PurchaseOrder order = new PurchaseOrder(number, customer);
        for (OrderStatus step : List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED)) {
            if (order.getStatus() == status) {
                break;
            }
            order.moveTo(step);
        }
        entityManager.persist(order);
        return order;
    }

}
