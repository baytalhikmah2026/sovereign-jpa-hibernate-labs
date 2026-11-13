package com.baytalhikmah.sovereign.web;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baytalhikmah.sovereign.service.ShopCustomers;
import com.baytalhikmah.sovereign.tenant.TenantContext;

/** The customer list of the shop named in the request's token. */
@RestController
@Profile("tenancy")
@RequestMapping("/api/customers")
public class ShopCustomerController {

    /** What a background worker found, and on which thread and for which tenant it worked. */
    public record BackgroundReport(String worker, String tenant, List<String> customers) {
    }

    private final ShopCustomers shopCustomers;

    private final TaskExecutor taskExecutor;

    public ShopCustomerController(ShopCustomers shopCustomers,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.shopCustomers = shopCustomers;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping
    public List<String> customers() {
        return shopCustomers.names();
    }

    @GetMapping("/background")
    public BackgroundReport customersFromBackground() {
        return CompletableFuture.supplyAsync(() -> new BackgroundReport(Thread.currentThread().getName(),
            TenantContext.get(), shopCustomers.names()), taskExecutor).join();
    }

}
