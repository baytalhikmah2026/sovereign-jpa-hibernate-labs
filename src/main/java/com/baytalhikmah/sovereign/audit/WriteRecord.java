package com.baytalhikmah.sovereign.audit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One line of the tamper-evident write log. It names who changed which record and which fields, never the values.
 * {@code hash} seals this record together with the hash of the record before it, so the lines form a chain.
 */
@JsonPropertyOrder({ "sequence", "at", "actor", "action", "entity", "entityId", "changed", "previousHash", "hash" })
public record WriteRecord(long sequence, String at, String actor, String action, String entity, String entityId,
        List<String> changed, String previousHash, String hash) {

    /** The exact text that is hashed: every field except the hash itself, in a fixed order. */
    public String sealedText() {
        return sequence + "|" + at + "|" + actor + "|" + action + "|" + entity + "|" + entityId + "|"
                + String.join(",", changed) + "|" + previousHash;
    }

}
