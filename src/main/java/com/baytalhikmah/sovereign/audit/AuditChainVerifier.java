package com.baytalhikmah.sovereign.audit;

import java.util.List;

import tools.jackson.databind.json.JsonMapper;

/** Re-computes the chain of a write-log segment and reports the first line that does not fit. */
public final class AuditChainVerifier {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private AuditChainVerifier() {
    }

    /** brokenLine is 0 when the whole chain is intact; otherwise it is the first bad line (counting from 1). */
    public record ChainReport(int records, int brokenLine, String reason) {

        public boolean intact() {
            return brokenLine == 0;
        }

    }

    public static ChainReport verify(List<String> lines) {
        String expectedPrevious = TamperEvidentLog.GENESIS;
        for (int i = 0; i < lines.size(); i++) {
            WriteRecord record = JSON.readValue(lines.get(i), WriteRecord.class);
            if (!record.previousHash().equals(expectedPrevious)) {
                return new ChainReport(lines.size(), i + 1, "previous hash does not match the line before");
            }
            if (!TamperEvidentLog.sha256(record.sealedText()).equals(record.hash())) {
                return new ChainReport(lines.size(), i + 1, "content does not match its hash");
            }
            expectedPrevious = record.hash();
        }
        return new ChainReport(lines.size(), 0, "intact");
    }

}
