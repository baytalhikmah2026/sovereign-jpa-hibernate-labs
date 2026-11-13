package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.domain.ShipmentReceipt;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Ships stock once per shipment key: the stock change and its receipt commit together or not at all. */
@Repository
public class ShipmentDesk {

    @PersistenceContext
    private EntityManager entityManager;

    /** Returns true when this call shipped, false when an earlier attempt with the same key already did. */
    @Transactional
    public boolean shipOnce(String shipmentKey, String warehouseCode, String sku, int quantity) {
        if (entityManager.find(ShipmentReceipt.class, shipmentKey) != null) {
            return false;
        }
        InventoryItem item = entityManager.find(InventoryItem.class, new InventoryItemId(warehouseCode, sku));
        item.ship(quantity);
        entityManager.persist(new ShipmentReceipt(shipmentKey, warehouseCode, sku, quantity));
        entityManager.flush();
        return true;
    }

}
