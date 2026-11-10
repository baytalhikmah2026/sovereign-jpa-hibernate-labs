package com.baytalhikmah.sovereign.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.domain.ProductManual;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Stores and reads product manuals as streams, 64 KB at a time, whatever the file size. */
@Service
public class ManualStore {

    private static final int BUFFER_SIZE = 64 * 1024;

    @PersistenceContext
    private EntityManager entityManager;

    /** Streams the data into a new large object; the file is never held in memory as a whole. */
    @Transactional
    public UUID store(UUID productId, String fileName, InputStream data, long sizeBytes) {
        Product product = entityManager.getReference(Product.class, productId);
        ProductManual manual = new ProductManual(product, fileName, sizeBytes,
            Hibernate.getLobHelper().createBlob(data, sizeBytes));
        entityManager.persist(manual);
        entityManager.flush();
        return manual.getId();
    }

    /** Streams the stored file back and returns its SHA-256 fingerprint and size. */
    @Transactional(readOnly = true)
    public String fingerprint(UUID manualId) {
        ProductManual manual = entityManager.find(ProductManual.class, manualId);
        try (InputStream in = manual.getContent().getBinaryStream()) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                sha256.update(buffer, 0, read);
                total += read;
            }
            return HexFormat.of().formatHex(sha256.digest()) + " (" + total + " bytes)";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot read manual " + manualId, e);
        }
    }

    /** Returns the manual entity; its content can only be read while a transaction is open. */
    @Transactional(readOnly = true)
    public ProductManual find(UUID manualId) {
        return entityManager.find(ProductManual.class, manualId);
    }

}
