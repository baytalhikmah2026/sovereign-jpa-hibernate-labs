package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.service.CustomerLifecycleService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PrimaryKeyTest {

    @Autowired
    CustomerLifecycleService service;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void customersGetTimeOrderedUuidV7Keys() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ids.add(service.register(new Customer("Customer " + i, "customer" + i + "@example.com")).getId());
        }

        System.out.println("=== UUIDv7 KEYS (in creation order) ===");
        for (UUID id : ids) {
            String created = jdbc.queryForObject(
                "select uuid_extract_timestamp(?::uuid)::text", String.class, id.toString());
            System.out.println(id + "  version " + id.version() + "  created " + created);
        }

        assertThat(ids).allSatisfy(id -> assertThat(id.version()).isEqualTo(7));
        assertThat(ids).isSortedAccordingTo(UUID::compareTo);
        assertThat(jdbc.queryForObject(
            "select data_type from information_schema.columns where table_name = 'sov_customer' and column_name = 'id'",
            String.class)).isEqualTo("uuid");
    }

    @Test
    @Transactional
    void timeOrderedKeysKeepTheIndexCompact() {
        jdbc.execute("create temporary table keys_random (id uuid primary key)");
        jdbc.execute("create temporary table keys_ordered (id uuid primary key)");
        jdbc.execute("insert into keys_random select uuidv4() from generate_series(1, 200000)");
        jdbc.execute("insert into keys_ordered select uuidv7() from generate_series(1, 200000)");

        long randomIndex = jdbc.queryForObject("select pg_relation_size('keys_random_pkey')", Long.class);
        long orderedIndex = jdbc.queryForObject("select pg_relation_size('keys_ordered_pkey')", Long.class);

        System.out.println("=== PRIMARY KEY INDEX SIZE FOR 200,000 ROWS ===");
        System.out.println("Random UUIDv4      : " + randomIndex / 1024 + " KB");
        System.out.println("Time-ordered UUIDv7: " + orderedIndex / 1024 + " KB");

        assertThat(orderedIndex).isLessThan(randomIndex);
    }

}
