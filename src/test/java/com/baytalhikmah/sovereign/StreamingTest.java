package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.query.CustomerExport;
import com.baytalhikmah.sovereign.query.ExportSummary;
import com.baytalhikmah.sovereign.service.CustomerSeedService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class StreamingTest {

    @Autowired
    CustomerSeedService seedService;

    @Autowired
    CustomerExport export;

    @Test
    void tenThousandCustomersPassThroughAFixedAmountOfMemory() {
        seedService.deleteSeedCustomers();
        seedService.insert(CustomerSeedService.generate(10_000, CustomerSeedService.SEED), 50);
        try {
            long start = System.nanoTime();
            ExportSummary streamed = export.streamCustomers(CustomerSeedService.SEED_DOMAIN, 500, 500);
            long streamMillis = (System.nanoTime() - start) / 1_000_000;

            start = System.nanoTime();
            ExportSummary scrolled = export.scrollChannels(CustomerSeedService.SEED_DOMAIN, 500);
            long scrollMillis = (System.nanoTime() - start) / 1_000_000;

            System.out.println("=== STREAMING 10,000 CUSTOMERS ===");
            System.out.println("Entity stream  : " + streamed.rows() + " rows in " + streamMillis + " ms, "
                + "at most " + streamed.mostTrackedAtOnce() + " entities in memory");
            System.out.println("Value scroll   : " + scrolled.rows() + " rows in " + scrollMillis + " ms, "
                + scrolled.mostTrackedAtOnce() + " entities in memory");
            System.out.println("Per channel    : " + streamed.perChannel());

            assertThat(streamed.rows()).isEqualTo(10_000);
            assertThat(streamed.mostTrackedAtOnce()).isLessThanOrEqualTo(500);
            assertThat(scrolled.rows()).isEqualTo(10_000);
            assertThat(scrolled.mostTrackedAtOnce()).isZero();
            assertThat(scrolled.perChannel()).isEqualTo(streamed.perChannel());
            assertThat(streamed.perChannel()).containsEntry("store", 2602L).containsEntry("web", 2481L);
        } finally {
            seedService.deleteSeedCustomers();
        }
    }

}
