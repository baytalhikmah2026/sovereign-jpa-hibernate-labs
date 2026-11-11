package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NamingStrategyTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void databaseNamesFollowTheSovereignRules() {
        List<String> tables = jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public' order by table_name",
            String.class);
        List<String> columns = jdbc.queryForList(
            "select column_name from information_schema.columns where table_name = 'sov_customer' order by column_name",
            String.class);
        List<String> sequences = jdbc.queryForList(
            "select sequence_name from information_schema.sequences where sequence_schema = 'public' order by sequence_name",
            String.class);

        System.out.println("=== NAMING REPORT ===");
        System.out.println("Tables    : " + tables);
        System.out.println("Columns   : " + columns);
        System.out.println("Sequences : " + sequences);

        assertThat(tables).contains("sov_customer").allMatch(name -> name.startsWith("sov_"));
        assertThat(columns).contains("email", "full_name", "id", "signup_channel");
        assertThat(sequences).containsExactly("sov_stock_movement_seq");
    }

}
