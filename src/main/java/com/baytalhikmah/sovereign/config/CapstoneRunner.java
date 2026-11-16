package com.baytalhikmah.sovereign.config;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.baytalhikmah.sovereign.audit.AuditChainVerifier;
import com.baytalhikmah.sovereign.audit.TamperEvidentLog;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.persistence.CustomerRepository;
import com.baytalhikmah.sovereign.persistence.ProductRepository;
import com.baytalhikmah.sovereign.service.CapstoneLoad;
import com.baytalhikmah.sovereign.service.CheckoutFlow;
import com.baytalhikmah.sovereign.service.GlobalStockDesk;
import com.baytalhikmah.sovereign.service.OrderDesk;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Profile capstone (together with cockroach): prepares shoppers, products and global stock if they are missing,
 * runs the checkout load test against the real Compose stack, and prints the report with the pass criteria.
 */
@Component
@Profile("capstone")
public class CapstoneRunner implements CommandLineRunner {

    static final String REGION = "eu-west";

    private final OrderDesk orders;
    private final GlobalStockDesk stock;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final DataSource dataSource;
    private final TamperEvidentLog writeLog;
    private final int shoppers;
    private final int checkouts;

    public CapstoneRunner(OrderDesk orders, GlobalStockDesk stock, CustomerRepository customers,
            ProductRepository products, DataSource dataSource, TamperEvidentLog writeLog,
            @Value("${sovereign.capstone.shoppers:8}") int shoppers,
            @Value("${sovereign.capstone.checkouts:400}") int checkouts) {
        this.orders = orders;
        this.stock = stock;
        this.customers = customers;
        this.products = products;
        this.dataSource = dataSource;
        this.writeLog = writeLog;
        this.shoppers = shoppers;
        this.checkouts = checkouts;
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String email = "buyer" + i + "@capstone.shop.test";
            if (customers.findByEmail(email).isEmpty()) {
                customers.save(new Customer("Capstone Buyer " + i, email));
            }
            emails.add(email);
        }
        List<String> skus = new ArrayList<>();
        int unitsBefore = 0;
        for (int i = 0; i < 8; i++) {
            String sku = "CAP-P" + i;
            if (products.findBySku(sku).isEmpty()) {
                products.save(new Product(sku, "Capstone product " + i, Money.of("25.00", "EUR")));
            }
            unitsBefore += stock.ensureStock(sku, REGION, "DE", 1_000_000);
            skus.add(sku);
        }

        String prefix = "R" + Long.toString(Instant.now().getEpochSecond(), 36).toUpperCase() + "-";
        CapstoneLoad.Report report =
            new CapstoneLoad(new CheckoutFlow(stock, orders, REGION)).run(shoppers, checkouts, prefix, emails, skus);

        int unitsAfter = 0;
        for (String sku : skus) {
            unitsAfter += stock.quantity(sku, REGION);
        }
        int active = dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
        boolean chainIntact = AuditChainVerifier.verify(Files.readAllLines(writeLog.segment())).intact();

        System.out.println("=== CAPSTONE: SOVEREIGN GLOBAL DATA GRID ===");
        print(report, shoppers, unitsBefore - unitsAfter, active, chainIntact);
    }

    /** The capstone report and its pass criteria; also used by the capstone test. */
    public static void print(CapstoneLoad.Report report, int shoppers, int unitsSold, int activeConnections,
            boolean chainIntact) {
        System.out.printf("checkouts          : %d by %d shoppers in %.1f s%n", report.checkouts(), shoppers,
            report.wallTime().toMillis() / 1000.0);
        System.out.printf("throughput         : %.1f checkouts/s (this machine)%n", report.throughputPerSecond());
        System.out.printf("latency p50 / p95  : %d ms / %d ms%n", report.p50().toMillis(), report.p95().toMillis());
        System.out.println("PASS no failed checkouts    : " + (report.failures() == 0) + " " + report.failureKinds());
        System.out.println("PASS stock matches orders   : " + (unitsSold == report.checkouts()) + " (" + unitsSold
                + " units / " + report.checkouts() + " orders)");
        System.out.println("PASS no connection left     : " + (activeConnections == 0));
        System.out.println("PASS audit chain intact     : " + chainIntact);
    }

}
