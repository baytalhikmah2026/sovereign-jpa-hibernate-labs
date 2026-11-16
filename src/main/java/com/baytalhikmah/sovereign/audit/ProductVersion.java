package com.baytalhikmah.sovereign.audit;

import java.time.Instant;
import java.util.Set;

/** One entry of a product's history: the state after a revision, who made it, and which fields it changed. */
public record ProductVersion(
        long revision,
        Instant changedAt,
        String changedBy,
        String action,
        String name,
        String price,
        Set<String> changedFields) {
}
