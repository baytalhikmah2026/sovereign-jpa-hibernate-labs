package com.baytalhikmah.sovereign.query;

/** One customer next to one of their orders; orderNumber is null for a customer without orders. */
public record CustomerOrderRow(String customerName, String orderNumber) {
}
