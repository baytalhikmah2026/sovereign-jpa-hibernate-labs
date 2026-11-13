package com.baytalhikmah.sovereign.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/** Tells Hibernate, each time a session opens, which tenant the session works for. */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.get();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

}
