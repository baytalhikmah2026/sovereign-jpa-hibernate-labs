package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;
import com.baytalhikmah.sovereign.service.StockDesk.StockLevel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/** Ships two items of one order together, locking both stock rows first. */
@Repository
public class PairedShipment {

    @PersistenceContext
    private EntityManager entityManager;

    /** Locks the items in the order the caller names them: two callers with opposite orders can deadlock. */
    @Transactional
    public List<StockLevel> shipPair(String warehouseCode, String firstSku, String secondSku, int quantity,
            Runnable betweenLocks) {
        return lockAndShip(warehouseCode, firstSku, secondSku, quantity, betweenLocks);
    }

    /** Always locks the items in alphabetical SKU order, whatever order the caller names them in. */
    @Transactional
    public List<StockLevel> shipPairInFixedOrder(String warehouseCode, String skuA, String skuB, int quantity,
            Runnable betweenLocks) {
        List<String> ordered = Stream.of(skuA, skuB).sorted().toList();
        return lockAndShip(warehouseCode, ordered.get(0), ordered.get(1), quantity, betweenLocks);
    }

    private List<StockLevel> lockAndShip(String warehouseCode, String firstSku, String secondSku, int quantity,
            Runnable betweenLocks) {
        InventoryItem first = lock(warehouseCode, firstSku);
        betweenLocks.run();
        InventoryItem second = lock(warehouseCode, secondSku);
        first.ship(quantity);
        second.ship(quantity);
        entityManager.flush();
        return List.of(StockDesk.levelOf(first), StockDesk.levelOf(second));
    }

    private InventoryItem lock(String warehouseCode, String sku) {
        System.out.println(Thread.currentThread().getName() + " locks " + sku);
        return entityManager.find(InventoryItem.class, new InventoryItemId(warehouseCode, sku),
            LockModeType.PESSIMISTIC_WRITE);
    }

}
