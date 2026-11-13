package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Asks PostgreSQL whether the current transaction runs on a replica (a server "in recovery"). */
@Repository
public class ReplicaProbe {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public boolean readTransactionRunsOnReplica() {
        return inRecovery();
    }

    @Transactional
    public boolean writeTransactionRunsOnReplica() {
        return inRecovery();
    }

    private boolean inRecovery() {
        return (Boolean) entityManager.createNativeQuery("select pg_is_in_recovery()").getSingleResult();
    }

}
