package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.persistence.PoolCredentials;
import com.zaxxer.hikari.util.Credentials;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CredentialRotationTest {

    private static final String VAULT_USER = "v-approle-sovereign-lab93";

    @Autowired
    PoolCredentials pool;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void thePoolMovesToAShortLivedUserWithoutInterruptingWork() throws Exception {
        Credentials fixed = pool.current();
        String expiration = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ"));
        runVaultStatements("scripts/vault-db-create-user.sql", "lab93-only-password", expiration);

        String busyBefore;
        String afterSwitch;
        String owner;
        try (Connection borrowed = dataSource.getConnection()) {
            pool.switchTo(VAULT_USER, "lab93-only-password");
            afterSwitch = jdbc.queryForObject("select session_user || ' as ' || current_user", String.class);
            jdbc.execute("create table sov_lab93_probe (id integer)");
            owner = jdbc.queryForObject("select tableowner from pg_tables where tablename = 'sov_lab93_probe'",
                String.class);
            jdbc.execute("drop table sov_lab93_probe");
            busyBefore = sessionUser(borrowed);
        }
        finally {
            pool.switchTo(fixed.getUsername(), fixed.getPassword());
        }
        String afterRestore = jdbc.queryForObject("select session_user", String.class);
        runVaultStatements("scripts/vault-db-drop-user.sql", null, null);
        Integer left = jdbc.queryForObject("select count(*) from pg_roles where rolname = ?", Integer.class, VAULT_USER);

        System.out.println("=== DYNAMIC DATABASE USER ===");
        System.out.println("fixed user          : " + fixed.getUsername());
        System.out.println("Vault-style user    : " + VAULT_USER + " (valid until " + expiration + ")");
        System.out.println("after the switch    : " + afterSwitch);
        System.out.println("borrowed before     : " + busyBefore + " (finished its work, then closed)");
        System.out.println("new table owned by  : " + owner);
        System.out.println("after revocation    : pool back on " + afterRestore + ", Vault user left: " + left);

        assertThat(afterSwitch).isEqualTo(VAULT_USER + " as sovereign_app");
        assertThat(busyBefore).isEqualTo("sovereign_app");
        assertThat(owner).isEqualTo("sovereign_app");
        assertThat(afterRestore).isEqualTo("sovereign_app");
        assertThat(left).isZero();
    }

    /** Fills in the placeholders the way Vault does and runs each statement of the file. */
    private void runVaultStatements(String file, String password, String expiration) throws Exception {
        String sql = Files.readString(Path.of(file)).replace("{{name}}", VAULT_USER);
        if (password != null) {
            sql = sql.replace("{{password}}", password).replace("{{expiration}}", expiration);
        }
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement.strip());
            }
        }
    }

    private static String sessionUser(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select session_user")) {
            result.next();
            return result.getString(1);
        }
    }

}
