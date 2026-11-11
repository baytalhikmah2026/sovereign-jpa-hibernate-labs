package com.baytalhikmah.sovereign.audit;

import java.time.Instant;

/** Marks an entity whose createdAt, updatedAt and changedBy fields are filled in by AuditStampInterceptor. */
public interface Audited {

    Instant getCreatedAt();

    Instant getUpdatedAt();

    String getChangedBy();

}
