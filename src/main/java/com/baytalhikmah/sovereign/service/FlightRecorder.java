package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.domain.ActionLogEntry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Writes the action log. Each entry can be kept in a transaction of its own or join the caller's transaction. */
@Service
public class FlightRecorder {

    @PersistenceContext
    private EntityManager entityManager;

    /** Always commits on its own: the caller's transaction waits, and a later rollback cannot undo the entry. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String detail) {
        write(action, detail);
    }

    /** Joins the caller's transaction (the default): the entry lives or dies with the caller's work. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordInCallerTransaction(String action, String detail) {
        write(action, detail);
    }

    private void write(String action, String detail) {
        entityManager.persist(new ActionLogEntry(CurrentActor.get(), action, detail));
        entityManager.flush();
        System.out.println("Log [" + action + "] written in transaction "
            + TransactionSynchronizationManager.getCurrentTransactionName());
    }

}
