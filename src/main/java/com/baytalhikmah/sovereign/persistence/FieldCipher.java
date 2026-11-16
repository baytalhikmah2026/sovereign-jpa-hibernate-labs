package com.baytalhikmah.sovereign.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM for single database fields. Stored form: "enc:v1:" + Base64(12-byte random nonce + ciphertext +
 * 16-byte tag). A new nonce for every value means the same text never looks the same twice. Values without the
 * prefix are old plain text and are returned unchanged, so existing rows stay readable while they are migrated.
 */
@Component
public class FieldCipher {

    static final String PREFIX = "enc:v1:";

    private static final Logger log = LoggerFactory.getLogger(FieldCipher.class);

    private static final int KEY_BYTES = 32;

    private static final int NONCE_BYTES = 12;

    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    private final SecretKey key;

    public FieldCipher(@Value("${sovereign.crypto.field-key:}") String base64Key) {
        byte[] keyBytes;
        if (base64Key.isBlank()) {
            keyBytes = new byte[KEY_BYTES];
            random.nextBytes(keyBytes);
            log.warn("sovereign.crypto.field-key is not set: using a temporary key. "
                    + "Encrypted fields written now cannot be read after a restart.");
        }
        else {
            keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != KEY_BYTES) {
                throw new IllegalArgumentException("sovereign.crypto.field-key must be 32 bytes (Base64)");
            }
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] stored = ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array();
            return PREFIX + Base64.getEncoder().encodeToString(stored);
        }
        catch (GeneralSecurityException failure) {
            throw new IllegalStateException("A field could not be encrypted", failure);
        }
    }

    public String decrypt(String stored) {
        if (!stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, NONCE_BYTES));
            return new String(cipher.doFinal(all, NONCE_BYTES, all.length - NONCE_BYTES), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException failure) {
            throw new IllegalStateException("An encrypted field could not be read (wrong key or changed data)", failure);
        }
    }

}
