package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.service.CustomerSeedService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class DeterministicInsertTest {

    static final String EXPECTED_FINGERPRINT = "2e53b30eb8542ef40da3b2d0fe9ac84f";

    @Autowired
    CustomerSeedService seedService;

    @Test
    void theGeneratorIsDeterministic() {
        List<String> first = CustomerSeedService.generate(10_000, CustomerSeedService.SEED)
            .stream().map(Customer::getEmail).toList();
        List<String> second = CustomerSeedService.generate(10_000, CustomerSeedService.SEED)
            .stream().map(Customer::getEmail).toList();

        System.out.println("=== DETERMINISTIC GENERATOR ===");
        System.out.println("First customer : " + first.get(0));
        System.out.println("Last customer  : " + first.get(9_999));

        assertThat(first).containsExactlyElementsOf(second);
    }

    @Test
    void tenThousandRowsGoInFastAndComplete() {
        long oneByOne = seedService.insert(CustomerSeedService.generate(1_000, CustomerSeedService.SEED), 1);
        seedService.deleteSeedCustomers();
        long inBatches = seedService.insert(CustomerSeedService.generate(1_000, CustomerSeedService.SEED), 50);
        seedService.deleteSeedCustomers();

        long milestone = seedService.insert(CustomerSeedService.generate(10_000, CustomerSeedService.SEED), 50);
        Map<String, Object> report = seedService.verify();

        System.out.println("=== 10,000-ROW MILESTONE ===");
        System.out.println("1,000 rows, one statement each : " + oneByOne + " ms");
        System.out.println("1,000 rows, batches of 50      : " + inBatches + " ms");
        System.out.println("10,000 rows, batches of 50     : " + milestone + " ms");
        System.out.println("Rows                           : " + report.get("rows"));
        System.out.println("Unique emails                  : " + report.get("unique_emails"));
        System.out.println("Fingerprint                    : " + report.get("fingerprint"));

        assertThat(inBatches).isLessThan(oneByOne);
        assertThat(report.get("rows")).isEqualTo(10_000L);
        assertThat(report.get("unique_emails")).isEqualTo(10_000L);
        assertThat(report.get("fingerprint")).isEqualTo(EXPECTED_FINGERPRINT);
    }

}
