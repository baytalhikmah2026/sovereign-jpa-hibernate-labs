package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.baytalhikmah.sovereign.service.ShopCustomers;
import com.baytalhikmah.sovereign.tenant.TenantContext;
import com.baytalhikmah.sovereign.tenant.TenantProvisioner;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("tenancy")
@SpringBootTest
class SchemaPerTenantTest {

    @Autowired
    TenantProvisioner provisioner;

    @Autowired
    ShopCustomers shopCustomers;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void eachShopSeesOnlyItsOwnCustomers() {
        provisioner.provision("nile");
        provisioner.provision("atlas");

        TenantContext.runAs("nile", () -> {
            shopCustomers.register("Amina Yusuf", "amina@nile.tenant.test");
            shopCustomers.register("Bilal Haddad", "bilal@nile.tenant.test");
        });
        TenantContext.runAs("atlas", () -> shopCustomers.register("Zara Malik", "zara@atlas.tenant.test"));

        List<String> nileCustomers = TenantContext.callAs("nile", shopCustomers::names);
        List<String> atlasCustomers = TenantContext.callAs("atlas", shopCustomers::names);
        String nileSchema = TenantContext.callAs("nile", shopCustomers::currentSchema);
        String platformSchema = shopCustomers.currentSchema();
        Integer shopCustomersInPublic = jdbcTemplate.queryForObject(
            "select count(*) from public.sov_customer where email like '%.tenant.test'", Integer.class);
        String poolSchemaAfterwards = jdbcTemplate.queryForObject("select current_schema()", String.class);
        Throwable forgedTenant = catchThrowable(() -> TenantContext.callAs("nile; drop schema", shopCustomers::names));

        System.out.println("=== SCHEMA PER TENANT ===");
        System.out.println("nile sees          : " + nileCustomers + " in " + nileSchema);
        System.out.println("atlas sees         : " + atlasCustomers);
        System.out.println("platform schema    : " + platformSchema + ", shop customers there: " + shopCustomersInPublic);
        System.out.println("pool after use     : " + poolSchemaAfterwards);
        System.out.println("forged tenant      : " + forgedTenant.getClass().getSimpleName());

        assertThat(nileCustomers).containsExactly("Amina Yusuf", "Bilal Haddad");
        assertThat(atlasCustomers).containsExactly("Zara Malik");
        assertThat(nileSchema).isEqualTo("shop_nile");
        assertThat(platformSchema).isEqualTo("public");
        assertThat(shopCustomersInPublic).isZero();
        assertThat(poolSchemaAfterwards).isEqualTo("public");
        assertThat(forgedTenant).hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Unknown tenant: nile; drop schema");
    }

}
