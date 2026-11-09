package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.service.CustomerLifecycleService;
import com.zaxxer.hikari.HikariDataSource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SqlInterceptionTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    CustomerLifecycleService service;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyStatementIsInterceptedWithItsRealValues() throws Exception {
        System.out.println("=== DATA SOURCE CHAIN ===");
        System.out.println("Outer : " + dataSource.getClass().getSimpleName());
        System.out.println("Pool  : " + dataSource.unwrap(HikariDataSource.class).getPoolName());

        System.out.println("=== INTERCEPTED SQL ===");
        Customer zara = service.register(new Customer("Zara Malik", "zara@example.com", "store"));
        service.findById(zara.getId());

        System.out.println("=== A DELIBERATELY SLOW QUERY ===");
        jdbc.queryForObject("select pg_sleep(0.3)::text", String.class);

        assertThat(dataSource.isWrapperFor(HikariDataSource.class)).isTrue();
    }

}
