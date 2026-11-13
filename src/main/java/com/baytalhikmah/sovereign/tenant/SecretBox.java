package com.baytalhikmah.sovereign.tenant;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Encrypts and decrypts short secrets with AES-256-GCM. The stored text is Base64 of
 * a random 12-byte nonce followed by the ciphertext and its 16-byte authentication tag.
 */
@Component
@Profile("tenancy")
@ConditionalOnProperty(name = "sovereign.tenancy.mode", havingValue = "database")
public class SecretBox {

    private static final int NONCE_BYTES = 12;

    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    private final SecureRandom random = new SecureRandom();

    public SecretBox(@Value("${sovereign.tenancy.registry-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("sovereign.tenancy.registry-key must be 32 bytes (Base64)");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] sealed = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + sealed.length)
                .put(nonce).put(sealed).array());
        }
        catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Could not encrypt", failure);
        }
    }

    public String decrypt(String storedText) {
        try {
            byte[] all = Base64.getDecoder().decode(storedText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, NONCE_BYTES));
            return new String(cipher.doFinal(all, NONCE_BYTES, all.length - NONCE_BYTES), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Could not decrypt: wrong key or changed data", failure);
        }
    }

}
