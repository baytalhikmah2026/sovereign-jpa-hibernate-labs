package com.baytalhikmah.sovereign.audit;

import org.hibernate.envers.RevisionListener;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * Called by Envers once for every new revision, on the thread that commits the transaction.
 * Copies the identity of that thread (actor, client address, current trace) into the revision row.
 */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        SpanContext span = Span.current().getSpanContext();
        ((AuditRevision) revisionEntity).recordIdentity(
                CurrentActor.get(),
                RequestOrigin.clientIp(),
                span.isValid() ? span.getTraceId() : null);
    }

}
