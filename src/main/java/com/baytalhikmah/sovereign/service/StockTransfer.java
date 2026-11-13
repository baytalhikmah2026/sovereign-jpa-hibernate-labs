package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.domain.ActionLogEntry;
import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.InventoryItemId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Moves stock between two warehouses in one transaction, and writes an action log entry for every attempt. */
@Service
public class StockTransfer {

    private final FlightRecorder flightRecorder;

    @PersistenceContext
    private EntityManager entityManager;

    public StockTransfer(FlightRecorder flightRecorder) {
        this.flightRecorder = flightRecorder;
    }

    /** The log entry is written in its own transaction, so it survives when the transfer fails. */
    @Transactional
    public void transfer(String from, String to, String sku, int quantity) {
        flightRecorder.record("transfer", describe(from, to, sku, quantity));
        move(from, to, sku, quantity);
    }

    /** The log entry joins this transaction, so a failed transfer takes the entry with it. */
    @Transactional
    public void transferLoggedInCallerTransaction(String from, String to, String sku, int quantity) {
        flightRecorder.recordInCallerTransaction("transfer-required", describe(from, to, sku, quantity));
        move(from, to, sku, quantity);
    }

    /** Calls its own REQUIRES_NEW method directly: the call never passes through Spring's proxy. */
    @Transactional
    public void transferWithSelfInvokedLog(String from, String to, String sku, int quantity) {
        recordHere("transfer-self-invoked", describe(from, to, sku, quantity));
        move(from, to, sku, quantity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHere(String action, String detail) {
        entityManager.persist(new ActionLogEntry(CurrentActor.get(), action, detail));
        entityManager.flush();
        System.out.println("Log [" + action + "] written in transaction "
            + TransactionSynchronizationManager.getCurrentTransactionName());
    }

    private void move(String from, String to, String sku, int quantity) {
        InventoryItem source = entityManager.find(InventoryItem.class, new InventoryItemId(from, sku));
        source.ship(quantity);
        entityManager.flush();
        InventoryItem target = entityManager.find(InventoryItem.class, new InventoryItemId(to, sku));
        if (target == null) {
            throw new IllegalStateException("No stock record of " + sku + " in " + to);
        }
        target.receive(quantity);
    }

    private static String describe(String from, String to, String sku, int quantity) {
        return from + " -> " + to + ": " + quantity + " x " + sku;
    }

}
