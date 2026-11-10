package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.ProductManual;
import com.baytalhikmah.sovereign.service.ManualStore;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LobStreamingTest {

    static final String KEY = "q49";
    static final long SIZE = 32L * 1024 * 1024;

    /** Produces the same bytes every time, one buffer at a time, without storing them. */
    static final class PatternStream extends InputStream {

        private final long size;
        private long position;

        PatternStream(long size) {
            this.size = size;
        }

        @Override
        public int read() {
            return position < size ? (int) ((position++ * 31 + 7) & 0xFF) : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (position >= size) {
                return -1;
            }
            int count = (int) Math.min(length, size - position);
            for (int i = 0; i < count; i++) {
                buffer[offset + i] = (byte) ((position++ * 31 + 7) & 0xFF);
            }
            return count;
        }

    }

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ManualStore manualStore;

    @Test
    void aLargeFileTravelsAsAStream() throws Exception {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, KEY));
        UUID kettleId = entityManager.createQuery("select p.id from Product p where p.sku = :sku", UUID.class)
            .setParameter("sku", QueryLabData.codePrefix(KEY) + "KET")
            .getSingleResult();

        long start = System.nanoTime();
        UUID manualId = manualStore.store(kettleId, "kettle-manual.bin", new PatternStream(SIZE), SIZE);
        long storeMillis = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        String fromDatabase = manualStore.fingerprint(manualId);
        long readMillis = (System.nanoTime() - start) / 1_000_000;
        String expected = expectedFingerprint();

        String columnType = jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_name = 'sov_product_manual' and column_name = 'content'""", String.class);
        ProductManual detached = manualStore.find(manualId);

        System.out.println("=== LOB STREAMING ===");
        System.out.println("Column type        : " + columnType + " (PostgreSQL large object)");
        System.out.println("Stored             : " + detached.getFileName() + ", " + detached.getSizeBytes()
            + " bytes in " + storeMillis + " ms");
        System.out.println("Read back          : " + fromDatabase + " in " + readMillis + " ms");
        System.out.println("Same as the source : " + fromDatabase.equals(expected));

        assertThat(columnType).isEqualTo("oid");
        assertThat(fromDatabase).isEqualTo(expected).endsWith("(33554432 bytes)");
        assertThatThrownBy(() -> detached.getContent().getBinaryStream().read())
            .satisfies(error -> assertThat(rootCause(error)).isInstanceOf(SQLException.class));
        System.out.println("Read after the transaction : refused");
    }

    private static String expectedFingerprint() throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        try (InputStream in = new PatternStream(SIZE)) {
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                sha256.update(buffer, 0, read);
                total += read;
            }
        }
        return HexFormat.of().formatHex(sha256.digest()) + " (" + total + " bytes)";
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

}
