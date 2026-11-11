package com.baytalhikmah.sovereign.service;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.StockMovement;
import com.baytalhikmah.sovereign.persistence.JdbcTripCounter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Writes stock movements in bulk and reports how often it had to ask the sequence for new IDs. */
@Service
public class StockLedger {

    static final int BATCH_SIZE = 50;

    /** Rows written, sequence calls, insert batches, and the first and last ID handed out. */
    public record IdReport(int rows, int sequenceCalls, int insertBatches, long firstId, long lastId) {
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public IdReport receiveDelivery(String skuPrefix, int movements) {
        JdbcTripCounter trips = new JdbcTripCounter();
        entityManager.unwrap(Session.class).addEventListeners(trips);
        long firstId = 0;
        long lastId = 0;
        for (int i = 1; i <= movements; i++) {
            StockMovement movement = new StockMovement(skuPrefix + (i % 6), 1, "delivery");
            entityManager.persist(movement);
            if (i == 1) {
                firstId = movement.getId();
            }
            lastId = movement.getId();
            if (i % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return new IdReport(movements, trips.statements(), trips.batches(), firstId, lastId);
    }

}
