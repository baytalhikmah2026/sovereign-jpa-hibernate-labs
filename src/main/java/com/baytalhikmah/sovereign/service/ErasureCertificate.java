package com.baytalhikmah.sovereign.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Proof of one GDPR erasure for the auditor. It names the customer only by ID (no personal data), lists what was
 * deleted and kept, and ends with a SHA-256 fingerprint of all these values.
 */
public record ErasureCertificate(
        UUID customerId,
        Instant erasedAt,
        Map<String, Integer> rowsDeleted,
        int ordersKeptAnonymised,
        int revisionsScrubbed,
        long remainingTraces,
        String fingerprint) {

    public boolean verified() {
        return remainingTraces == 0;
    }

}
