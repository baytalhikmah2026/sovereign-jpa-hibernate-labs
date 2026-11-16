package com.baytalhikmah.sovereign.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;

/**
 * Changes the user name and password of the running connection pool. New connections log in with the new user at
 * once; connections that are busy finish their work and are then closed, so nothing is interrupted.
 */
@Component
public class PoolCredentials {

    private static final Logger log = LoggerFactory.getLogger(PoolCredentials.class);

    private final DataSource dataSource;

    public PoolCredentials(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Credentials current() {
        return hikari().getCredentials();
    }

    public void switchTo(String username, String password) {
        HikariDataSource hikari = hikari();
        String previous = hikari.getUsername();
        hikari.getHikariConfigMXBean().setCredentials(Credentials.of(username, password));
        hikari.getHikariPoolMXBean().softEvictConnections();
        log.info("Connection pool switched from database user {} to {}", previous, username);
    }

    private HikariDataSource hikari() {
        try {
            return dataSource.unwrap(HikariDataSource.class);
        }
        catch (SQLException failure) {
            throw new IllegalStateException("The DataSource is not a Hikari pool", failure);
        }
    }

}
