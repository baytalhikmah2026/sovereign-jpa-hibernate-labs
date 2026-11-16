package com.baytalhikmah.sovereign.audit;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.type.Type;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baytalhikmah.sovereign.audit.TamperEvidentLog.PendingWrite;

/**
 * The application's one Hibernate interceptor: it still stamps Audited entities (Lab 66) and now also notes every
 * insert, update and delete of a domain entity made in a Spring-managed transaction. The notes are collected per
 * transaction and written to the tamper-evident log only after Spring reports a successful commit; a rollback
 * leaves no trace.
 */
public class WriteTrailInterceptor extends AuditStampInterceptor {

    private static final String DOMAIN_PACKAGE = "com.baytalhikmah.sovereign.domain";

    private final TamperEvidentLog log;

    public WriteTrailInterceptor(TamperEvidentLog log) {
        this.log = log;
    }

    @Override
    public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        boolean stamped = super.onPersist(entity, id, state, propertyNames, types);
        note(entity, id, "INSERT", List.of());
        return stamped;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
            String[] propertyNames, Type[] types) {
        boolean stamped = super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        List<String> changed = new ArrayList<>();
        for (int i = 0; i < propertyNames.length; i++) {
            if (previousState != null && !types[i].isEqual(currentState[i], previousState[i])) {
                changed.add(propertyNames[i]);
            }
        }
        note(entity, id, "UPDATE", changed);
        return stamped;
    }

    @Override
    public void onRemove(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        note(entity, id, "DELETE", List.of());
    }

    private void note(Object entity, Object id, String action, List<String> changed) {
        if (!entity.getClass().getPackageName().equals(DOMAIN_PACKAGE)
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        batchOfCurrentTransaction().writes.add(new PendingWrite(CurrentActor.get(), action,
            entity.getClass().getSimpleName(), String.valueOf(id), List.copyOf(changed)));
    }

    /** The batch registered with the current Spring transaction, created on its first write. */
    private WriteBatch batchOfCurrentTransaction() {
        for (TransactionSynchronization registered : TransactionSynchronizationManager.getSynchronizations()) {
            if (registered instanceof WriteBatch batch && batch.owner() == this) {
                return batch;
            }
        }
        WriteBatch batch = new WriteBatch();
        TransactionSynchronizationManager.registerSynchronization(batch);
        return batch;
    }

    /** The writes of one transaction; Spring calls afterCommit only when the commit succeeded. */
    private final class WriteBatch implements TransactionSynchronization {

        private final List<PendingWrite> writes = new ArrayList<>();

        private WriteTrailInterceptor owner() {
            return WriteTrailInterceptor.this;
        }

        @Override
        public void afterCommit() {
            log.append(writes);
        }

    }

}
