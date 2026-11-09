package com.baytalhikmah.sovereign.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("vault")
public class VaultConnectionReport implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final Environment environment;

    public VaultConnectionReport(JdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        String password = environment.getProperty("spring.datasource.password", "");
        System.out.println("=== VAULT CONNECTION REPORT ===");
        System.out.println("Credentials from : Vault (secret/sovereign-persistence)");
        System.out.println("Database user    : " + jdbc.queryForObject("select current_user", String.class));
        System.out.println("Database         : " + jdbc.queryForObject("select current_database()", String.class));
        System.out.println("Password length  : " + password.length() + " characters (never printed)");
    }

}
