package com.baytalhikmah.sovereign.audit;

import java.time.Instant;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/** One interceptor for the whole application: stamps every Audited entity before Hibernate writes it. */
public class AuditStampInterceptor implements Interceptor {

    @Override
    public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (!(entity instanceof Audited)) {
            return false;
        }
        Instant now = Instant.now();
        set(state, propertyNames, "createdAt", now);
        set(state, propertyNames, "updatedAt", now);
        set(state, propertyNames, "changedBy", CurrentActor.get());
        return true;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
            String[] propertyNames, Type[] types) {
        if (!(entity instanceof Audited)) {
            return false;
        }
        set(currentState, propertyNames, "updatedAt", Instant.now());
        set(currentState, propertyNames, "changedBy", CurrentActor.get());
        return true;
    }

    private static void set(Object[] state, String[] propertyNames, String property, Object value) {
        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(property)) {
                state[i] = value;
                return;
            }
        }
        throw new IllegalStateException("Audited entity has no property " + property);
    }

}
