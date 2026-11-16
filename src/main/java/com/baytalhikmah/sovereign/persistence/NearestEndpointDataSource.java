package com.baytalhikmah.sovereign.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Sends every connection request to the cluster endpoint (gateway node) with the lowest measured round-trip time.
 * The application copy in each region measures for itself, so it always talks to the node nearest to it.
 * Call {@link #refresh()} again when the network changes.
 */
public class NearestEndpointDataSource extends AbstractRoutingDataSource implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NearestEndpointDataSource.class);

    private static final int SAMPLES = 5;

    private final Map<String, DataSource> endpoints;

    private volatile String nearest;

    public NearestEndpointDataSource(Map<String, DataSource> endpoints) {
        this.endpoints = new LinkedHashMap<>(endpoints);
        setTargetDataSources(new HashMap<>(endpoints));
        afterPropertiesSet();
        refresh();
    }

    /** Measures every endpoint (median of five round trips) and switches to the fastest one. */
    public synchronized Map<String, Duration> refresh() {
        Map<String, Duration> measured = new LinkedHashMap<>();
        endpoints.forEach((region, endpoint) -> measured.put(region, roundTrip(endpoint)));
        String fastest = measured.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();
        if (!fastest.equals(nearest)) {
            log.info("Nearest cluster endpoint is now {} (round trips: {})", fastest, inMillis(measured));
        }
        nearest = fastest;
        return measured;
    }

    public String nearest() {
        return nearest;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return nearest;
    }

    @Override
    public void close() {
        endpoints.values().forEach(endpoint -> {
            if (endpoint instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                }
                catch (Exception ignored) {
                    // closing on shutdown: nothing more to do
                }
            }
        });
    }

    public static Map<String, String> inMillis(Map<String, Duration> measured) {
        Map<String, String> readable = new LinkedHashMap<>();
        measured.forEach((region, time) -> readable.put(region, String.format("%.1f ms", time.toNanos() / 1_000_000.0)));
        return readable;
    }

    private static Duration roundTrip(DataSource endpoint) {
        long[] nanos = new long[SAMPLES];
        try (Connection connection = endpoint.getConnection(); Statement statement = connection.createStatement()) {
            for (int i = 0; i < SAMPLES; i++) {
                long start = System.nanoTime();
                statement.execute("select 1");
                nanos[i] = System.nanoTime() - start;
            }
        }
        catch (SQLException unreachable) {
            return Duration.ofDays(1);
        }
        Arrays.sort(nanos);
        return Duration.ofNanos(nanos[SAMPLES / 2]);
    }

}
