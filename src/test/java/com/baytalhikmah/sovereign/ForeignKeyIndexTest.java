package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ForeignKeyIndexTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyForeignKeyHasAnIndex() throws Exception {
        String report = Files.readString(Path.of("scripts", "fk-index-report.sql"));
        List<String> unindexed = jdbc.query(report,
            (row, n) -> row.getString("child_table") + "." + row.getString("fk_column") + " → " + row.getString("parent_table"));
        Integer foreignKeys = jdbc.queryForObject(
            "select count(*) from pg_constraint where contype = 'f' and connamespace = 'public'::regnamespace", Integer.class);

        System.out.println("=== RELATIONAL INTEGRITY REPORT ===");
        System.out.println("Foreign keys enforced : " + foreignKeys);
        System.out.println("Without an index      : " + unindexed.size());
        unindexed.forEach(line -> System.out.println("  " + line));

        assertThat(unindexed).isEmpty();
    }

}
