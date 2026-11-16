package com.baytalhikmah.sovereign.config;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultOperations;

/**
 * Proves the secured set-up at start-up: the database session is encrypted and the client certificate was
 * checked, and Vault Transit encrypts and decrypts with a key that never leaves Vault.
 */
@Component
@Profile("tls")
public class TlsConnectionReport implements CommandLineRunner {

    static final String FIELD_KEY = "sovereign-fields";

    private final JdbcTemplate jdbc;

    private final ObjectProvider<VaultOperations> vault;

    public TlsConnectionReport(JdbcTemplate jdbc, ObjectProvider<VaultOperations> vault) {
        this.jdbc = jdbc;
        this.vault = vault;
    }

    @Override
    public void run(String... args) {
        Map<String, Object> session = jdbc.queryForMap(
                "select ssl, version, cipher, client_dn from pg_stat_ssl where pid = pg_backend_pid()");
        System.out.println("=== TLS CONNECTION REPORT ===");
        System.out.println("Encrypted        : " + session.get("ssl"));
        System.out.println("Protocol         : " + session.get("version"));
        System.out.println("Cipher           : " + session.get("cipher"));
        System.out.println("Client identity  : " + session.get("client_dn"));
        System.out.println("Database user    : " + jdbc.queryForObject("select current_user", String.class));

        VaultOperations operations = vault.getIfAvailable();
        if (operations == null) {
            System.out.println("Key service      : not connected (start with the vault profile too)");
            return;
        }
        String secret = "GB82 WEST 1234 5698 7654 32";
        String ciphertext = operations.opsForTransit().encrypt(FIELD_KEY, secret);
        String plaintext = operations.opsForTransit().decrypt(FIELD_KEY, ciphertext);
        System.out.println("Key service      : Vault Transit, key " + FIELD_KEY);
        System.out.println("Encrypted sample : " + ciphertext.substring(0, Math.min(30, ciphertext.length())) + "...");
        System.out.println("Round trip       : " + (plaintext.equals(secret) ? "OK" : "MISMATCH"));
    }

}
