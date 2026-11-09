package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SovereignSandboxTest {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void sandboxIsAlive() {
		String database = jdbc.queryForObject("select version()", String.class);
		String user = jdbc.queryForObject("select current_user", String.class);

		System.out.println("=== SOVEREIGN SANDBOX ONLINE ===");
		System.out.println("Database  : " + database);
		System.out.println("Connected : " + user);
		System.out.println("Hibernate : " + Version.getVersionString());
		System.out.println("Java      : " + Runtime.version());

		assertThat(database).startsWith("PostgreSQL 18");
		assertThat(user).isEqualTo("sovereign_app");
		assertThat(Version.getVersionString()).startsWith("7.2");
	}

}
