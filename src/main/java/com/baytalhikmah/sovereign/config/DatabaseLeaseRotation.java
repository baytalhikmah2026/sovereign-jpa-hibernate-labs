package com.baytalhikmah.sovereign.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

import com.baytalhikmah.sovereign.persistence.PoolCredentials;

/**
 * Keeps the connection pool on a valid Vault database user. Spring Cloud Vault renews the user's lease until it
 * reaches its maximum lifetime; then this class asks Vault for a new user, and every new user from then on is
 * handed to the pool.
 */
@Component
@Profile("dynamic-db")
public class DatabaseLeaseRotation {

    private static final Logger log = LoggerFactory.getLogger(DatabaseLeaseRotation.class);

    public DatabaseLeaseRotation(SecretLeaseContainer leases, PoolCredentials pool,
            @Value("${spring.cloud.vault.database.backend:database}") String backend,
            @Value("${spring.cloud.vault.database.role}") String role) {
        String path = backend + "/creds/" + role;
        leases.addLeaseListener(event -> {
            if (!path.equals(event.getSource().getPath())) {
                return;
            }
            if (event instanceof SecretLeaseExpiredEvent && event.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                log.info("The lease of {} reached its maximum lifetime: asking Vault for a new database user", path);
                leases.requestRotatingSecret(path);
            }
            else if (event instanceof SecretLeaseCreatedEvent created
                    && event.getSource().getMode() == RequestedSecret.Mode.ROTATE) {
                pool.switchTo((String) created.getSecrets().get("username"),
                    (String) created.getSecrets().get("password"));
            }
        });
    }

}
