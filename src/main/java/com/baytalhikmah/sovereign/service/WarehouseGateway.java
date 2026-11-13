package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.baytalhikmah.sovereign.domain.StockChanged;

/**
 * Stands in for the client of the external warehouse system (in a real shop an HTTP or message call).
 * It prints each message and keeps it, so you can see exactly what the other system received.
 */
@Component
public class WarehouseGateway {

    private final List<String> sent = new CopyOnWriteArrayList<>();

    public void send(StockChanged change) {
        String message = change.warehouseCode() + "/" + change.sku() + " on hand " + change.quantityOnHand()
            + " (version " + change.version() + ")";
        System.out.println("WMS <- " + message);
        sent.add(message);
    }

    /** The messages sent so far for one warehouse, oldest first. */
    public List<String> sentFor(String warehouseCode) {
        return sent.stream().filter(message -> message.startsWith(warehouseCode + "/")).toList();
    }

}
