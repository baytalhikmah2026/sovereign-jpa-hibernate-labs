package com.baytalhikmah.sovereign.query;

import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Statements that take as long as we tell them to: pg_sleep stands in for a runaway report. */
@Repository
public class SlowQueries {

    private static final String SLOW_STATEMENT = "select 1 from pg_sleep(:seconds)";

    @PersistenceContext
    private EntityManager entityManager;

    /** Timeout on one query, in milliseconds. */
    public Object runWithQueryTimeout(double seconds, int timeoutMillis) {
        return entityManager.createNativeQuery(SLOW_STATEMENT)
            .setParameter("seconds", seconds)
            .setHint(SpecHints.HINT_SPEC_QUERY_TIMEOUT, timeoutMillis)
            .getSingleResult();
    }

    /** Timeout on the whole transaction, in seconds; Spring passes the time left on to each query. */
    @Transactional(readOnly = true, timeout = 1)
    public Object runInShortTransaction(double seconds) {
        return entityManager.createNativeQuery(SLOW_STATEMENT)
            .setParameter("seconds", seconds)
            .getSingleResult();
    }

    /** No timeout written here: the application-wide default from application.yaml applies. */
    public Object runWithDefaultTimeout(double seconds) {
        return entityManager.createNativeQuery(SLOW_STATEMENT)
            .setParameter("seconds", seconds)
            .getSingleResult();
    }

}
