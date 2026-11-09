package com.baytalhikmah.sovereign.config;

import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.baytalhikmah.sovereign.service.CustomerSeedService;

@Component
@Profile("seed")
public class SeedRunner implements CommandLineRunner {

    private final CustomerSeedService seedService;

    public SeedRunner(CustomerSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(String... args) {
        if (((Number) seedService.verify().get("rows")).longValue() > 0) {
            System.out.println("=== SEED SKIPPED: seed customers already exist ===");
        }
        else {
            long millis = seedService.insert(CustomerSeedService.generate(10_000, CustomerSeedService.SEED), 50);
            System.out.println("=== SEED LOADED: 10,000 customers in " + millis + " ms ===");
        }
        Map<String, Object> report = seedService.verify();
        System.out.println("Rows          : " + report.get("rows"));
        System.out.println("Unique emails : " + report.get("unique_emails"));
        System.out.println("Fingerprint   : " + report.get("fingerprint"));
    }

}
