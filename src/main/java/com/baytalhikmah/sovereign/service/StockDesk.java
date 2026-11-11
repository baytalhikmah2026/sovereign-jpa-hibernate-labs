package com.baytalhikmah.sovereign.service;

import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Timeouts;
import org.hibernate.jpa.SpecHints;
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

    /** Like shipLocked, but waits at most {@code maxWait} for the lock (zero = do not wait at all). */
    @Transactional
    public StockLevel shipWithin(String warehouseCode, String sku, int quantity, Duration maxWait) {
        InventoryItemId id = new InventoryItemId(warehouseCode, sku);
        InventoryItem item;
        if (maxWait.isZero()) {
            item = entityManager.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE,
                Map.of(SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.NO_WAIT_MILLI));
        }
        else {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("set local lock_timeout = " + maxWait.toMillis());
                }
            });
            item = entityManager.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE);
        }
        item.ship(quantity);
        entityManager.flush();
        return levelOf(item);
    }

    /** Locks up to {@code max} items of a warehouse, skipping rows another transaction has locked. */
    @Transactional
    public List<String> claimUnlockedSkus(String warehouseCode, int max) {
        return entityManager.createQuery(
                "select i from InventoryItem i where i.warehouseCode = :warehouse order by i.sku", InventoryItem.class)
            .setParameter("warehouse", warehouseCode)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED_MILLI)
            .setMaxResults(max)
            .getResultList()
            .stream()
            .map(InventoryItem::getSku)
            .toList();
    }

    static StockLevel levelOf(InventoryItem item) {
        return new StockLevel(item.getWarehouseCode(), item.getSku(), item.getQuantityOnHand(), item.getVersion());
    }

}
