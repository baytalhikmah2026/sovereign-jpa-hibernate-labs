package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.persistence.ConnectionLeases;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class LeakRecoveryTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    ConnectionLeases leases;

    @Test
    void theWatchdogTakesBackAForgottenConnection(CapturedOutput output) throws Exception {
        HikariPoolMXBean pool = dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean();
        int activeBefore = pool.getActiveConnections();

        AtomicReference<String> workerSaw = new AtomicReference<>("nothing");
        Thread careless = Thread.ofPlatform().name("careless-report-1").start(() -> {
            try {
                Connection connection = leases.borrow("nightly report");
                connection.createStatement().execute("select 1");
                try {
                    Thread.sleep(Duration.ofSeconds(30));
                    workerSaw.set("finished sleeping");
                }
                catch (InterruptedException stopped) {
                    workerSaw.set("interrupted, connection closed: " + connection.isClosed());
                }
            }
            catch (Exception failed) {
                workerSaw.set("error: " + failed);
            }
        });

        Thread.sleep(2500);
        int activeWhileLeaking = pool.getActiveConnections();
        List<String> overdue = leases.overdue(Duration.ofSeconds(2));
        List<String> reclaimed = leases.reclaimOverdue(Duration.ofSeconds(2));
        careless.join(Duration.ofSeconds(5));
        int activeAfter = pool.getActiveConnections();

        System.out.println("=== LEAK RECOVERY ===");
        System.out.println("active connections : " + activeBefore + " -> " + activeWhileLeaking + " -> " + activeAfter);
        System.out.println("overdue leases     : " + overdue);
        System.out.println("reclaimed          : " + reclaimed);
        System.out.println("worker saw         : " + workerSaw.get());
        System.out.println("pool warned        : " + output.getOut().contains("Connection leak detection triggered"));

        assertThat(activeWhileLeaking).isEqualTo(activeBefore + 1);
        assertThat(overdue).containsExactly("careless-report-1 (nightly report)");
        assertThat(reclaimed).containsExactly("careless-report-1 (nightly report)");
        assertThat(workerSaw.get()).isEqualTo("interrupted, connection closed: true");
        assertThat(activeAfter).isEqualTo(activeBefore);
        assertThat(output.getOut()).contains("Connection leak detection triggered", "careless-report-1");
    }

}
