package com.baytalhikmah.sovereign.persistence;

import org.hibernate.SessionEventListener;

/** Counts how a session talks to the database: single statements and whole batches. */
public class JdbcTripCounter implements SessionEventListener {

    private int statements;
    private int batches;

    @Override
    public void jdbcExecuteStatementStart() {
        statements++;
    }

    @Override
    public void jdbcExecuteBatchStart() {
        batches++;
    }

    public int statements() {
        return statements;
    }

    public int batches() {
        return batches;
    }

}
