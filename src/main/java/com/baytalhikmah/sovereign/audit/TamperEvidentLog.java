package com.baytalhikmah.sovereign.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Append-only, hash-chained log of every committed write. Each application start opens a new segment file
 * (audit-TIME-ID.jsonl). The writes of a commit are written and forced to disk before the call returns, so the
 * record leaves the application's memory at once; a sidecar copies new lines to write-once storage.
 */
@Component
public class TamperEvidentLog {

    static final String GENESIS = "0".repeat(64);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final DateTimeFormatter SEGMENT_TIME =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final Path segment;

    private long sequence;

    private String previousHash = GENESIS;

    public TamperEvidentLog(@Value("${sovereign.audit.log-dir:logs/audit}") String directory) {
        String name = "audit-" + SEGMENT_TIME.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8)
                + ".jsonl";
        this.segment = Path.of(directory).toAbsolutePath().resolve(name);
    }

    /** One write as seen by Hibernate, before it is sealed into the chain. */
    public record PendingWrite(String actor, String action, String entity, String entityId, List<String> changed) {
    }

    /** Seals the writes of one committed transaction and forces them to disk in a single step. */
    public synchronized void append(List<PendingWrite> writes) {
        if (writes.isEmpty()) {
            return;
        }
        StringBuilder lines = new StringBuilder();
        for (PendingWrite write : writes) {
            sequence++;
            WriteRecord unsealed = new WriteRecord(sequence, Instant.now().toString(), write.actor(), write.action(),
                write.entity(), write.entityId(), write.changed(), previousHash, null);
            String hash = sha256(unsealed.sealedText());
            WriteRecord sealed = new WriteRecord(unsealed.sequence(), unsealed.at(), unsealed.actor(),
                unsealed.action(), unsealed.entity(), unsealed.entityId(), unsealed.changed(), previousHash, hash);
            lines.append(JSON.writeValueAsString(sealed)).append('\n');
            previousHash = hash;
        }
        try {
            Files.createDirectories(segment.getParent());
            try (FileChannel file = FileChannel.open(segment, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND)) {
                file.write(ByteBuffer.wrap(lines.toString().getBytes(StandardCharsets.UTF_8)));
                file.force(false);
            }
        }
        catch (IOException failure) {
            throw new UncheckedIOException("The write log could not be written: " + segment, failure);
        }
    }

    public Path segment() {
        return segment;
    }

    static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

}
