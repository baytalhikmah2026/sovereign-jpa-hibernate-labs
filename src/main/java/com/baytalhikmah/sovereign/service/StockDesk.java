package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/**
 * The warehouse desk that ships stock. @Repository also turns database and JPA exceptions
 * into Spring's DataAccessException family, so callers can catch one set of exception types.
 */
@Repository
public class StockDesk {

    /** Stock of one item after a change, with its version number. */
    public record StockLevel(String warehouseCode, String sku, int quantityOnHand, long version) {
    }

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Ships stock in one transaction. {@code thinkingTime} runs after the item was read and before it is
     * changed: it stands for a user who takes a moment, during which a colleague may ship the same item.
     */
    @Transactional
    public StockLevel shipOnce(String warehouseCode, String sku, int quantity, Runnable thinkingTime) {
        InventoryItem item = entityManager.find(InventoryItem.class, new InventoryItemId(warehouseCode, sku));
        thinkingTime.run();
        item.ship(quantity);
        entityManager.flush();
        return levelOf(item);
    }

    /** Locks the row for writing (other writers and readers with a lock must wait), then ships. */
    @Transactional
    public StockLevel shipLocked(String warehouseCode, String sku, int quantity, Runnable whileLocked) {
        InventoryItem item = entityManager.find(InventoryItem.class, new InventoryItemId(warehouseCode, sku),
            LockModeType.PESSIMISTIC_WRITE);
        whileLocked.run();
        item.ship(quantity);
        entityManager.flush();
        return levelOf(item);
    }

    /** Locks the row for reading: other readers may lock it too, writers must wait. */
    @Transactional
    public StockLevel countLocked(String warehouseCode, String sku, Runnable whileLocked) {
        InventoryItem item = entityManager.find(InventoryItem.class, new InventoryItemId(warehouseCode, sku),
            LockModeType.PESSIMISTIC_READ);
        whileLocked.run();
        return levelOf(item);
    }

    static StockLevel levelOf(InventoryItem item) {
        return new StockLevel(item.getWarehouseCode(), item.getSku(), item.getQuantityOnHand(), item.getVersion());
    }

}
