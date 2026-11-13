package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.baytalhikmah.sovereign.service.ShopCustomers;
import com.baytalhikmah.sovereign.tenant.DatabaseProvisioner;
import com.baytalhikmah.sovereign.tenant.DatabaseRouter;
import com.baytalhikmah.sovereign.tenant.TenantContext;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("tenancy")
@SpringBootTest(properties = {
    "sovereign.tenancy.mode=database",
    "sovereign.tenancy.registry-key=c292ZXJlaWduLWxhYi03Mi10ZXN0LWtleS0zMi1ieXQ="
})
class DatabasePerTenantTest {

    @Autowired
    DatabaseProvisioner provisioner;

    @Autowired
    DatabaseRouter router;

    @Autowired
    ShopCustomers shopCustomers;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcConnectionDetails platform;

    @Test
    void eachShopLivesInItsOwnDatabase() throws Exception {
        provisioner.provision("orion");
        provisioner.provision("vega");

        TenantContext.runAs("orion", () -> shopCustomers.register("Omar Farouk", "omar@orion.tenant.test"));
        TenantContext.runAs("vega", () -> {
            shopCustomers.register("Layla Hassan", "layla@vega.tenant.test");
            shopCustomers.register("Yusuf Kareem", "yusuf@vega.tenant.test");
        });

        List<String> orionCustomers = TenantContext.callAs("orion", shopCustomers::names);
        List<String> vegaCustomers = TenantContext.callAs("vega", shopCustomers::names);
        String orionDatabase = TenantContext.callAs("orion", shopCustomers::currentDatabase);
        String platformDatabase = shopCustomers.currentDatabase();
        String storedSecret = jdbcTemplate.queryForObject(
            "select password_ciphertext from sov_tenant_database where tenant_id = 'orion'", String.class);
        Throwable unregistered = catchThrowable(() -> TenantContext.callAs("pluto", shopCustomers::names));

        System.out.println("=== DATABASE PER TENANT ===");
        System.out.println("orion sees        : " + orionCustomers + " in database " + orionDatabase);
        System.out.println("vega sees         : " + vegaCustomers);
        System.out.println("platform database : " + platformDatabase);
        System.out.println("stored secret     : " + storedSecret.substring(0, 16) + "... (" + storedSecret.length() + " characters)");
        System.out.println("open shop pools   : " + router.openPools());
        System.out.println("unregistered shop : " + unregistered.getClass().getSimpleName());

        assertThat(orionCustomers).containsExactly("Omar Farouk");
        assertThat(vegaCustomers).containsExactly("Layla Hassan", "Yusuf Kareem");
        assertThat(orionDatabase).isEqualTo("shop_orion");
        assertThat(platformDatabase).isEqualTo("sovereign");
        assertThat(storedSecret).doesNotContain(platform.getPassword());
        assertThat(router.openPools()).containsExactly("orion", "vega");
        assertThat(unregistered).hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Unknown tenant: pluto");
    }

}
