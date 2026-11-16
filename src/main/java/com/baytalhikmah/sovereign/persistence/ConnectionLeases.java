package com.baytalhikmah.sovereign.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hands out raw JDBC connections on a lease. A watchdog takes back every connection held longer than allowed:
 * it closes the connection (so the pool gets its slot back) and interrupts the thread that forgot it.
 */
@Component
public class ConnectionLeases {

    private record Lease(Thread holder, String purpose, Instant borrowedAt) {

        String describe() {
            return holder.getName() + " (" + purpose + ")";
        }

    }

    private final DataSource dataSource;

    private final Duration maxHold;

    private final Map<Connection, Lease> leases = new ConcurrentHashMap<>();

    public ConnectionLeases(DataSource dataSource,
            @Value("${sovereign.leak-guard.max-hold-ms:10000}") long maxHoldMillis) {
        this.dataSource = dataSource;
        this.maxHold = Duration.ofMillis(maxHoldMillis);
    }

    public Connection borrow(String purpose) throws SQLException {
        Connection connection = dataSource.getConnection();
        leases.put(connection, new Lease(Thread.currentThread(), purpose, Instant.now()));
        return connection;
    }

    public void giveBack(Connection connection) throws SQLException {
        leases.remove(connection);
        connection.close();
    }

    /** Who holds a connection longer than the limit. */
    public List<String> overdue(Duration limit) {
        Instant cutoff = Instant.now().minus(limit);
        return leases.values().stream().filter(lease -> lease.borrowedAt().isBefore(cutoff)).map(Lease::describe)
            .toList();
    }

    /** Runs every second when scheduling is on (profile leak-guard). */
    @Scheduled(fixedDelayString = "${sovereign.leak-guard.check-interval-ms:1000}")
    public void reclaimOverdue() {
        reclaimOverdue(maxHold);
    }

    /** Closes every connection held longer than the limit and interrupts its holder; returns who was stopped. */
    public List<String> reclaimOverdue(Duration limit) {
        Instant cutoff = Instant.now().minus(limit);
        List<String> reclaimed = new ArrayList<>();
        leases.forEach((connection, lease) -> {
            if (lease.borrowedAt().isBefore(cutoff) && leases.remove(connection, lease)) {
                try {
                    connection.close();
                }
                catch (SQLException closeFailed) {
                    // the connection is broken anyway; the pool replaces broken connections itself
                }
                lease.holder().interrupt();
                reclaimed.add(lease.describe());
            }
        });
        return reclaimed;
    }

}
