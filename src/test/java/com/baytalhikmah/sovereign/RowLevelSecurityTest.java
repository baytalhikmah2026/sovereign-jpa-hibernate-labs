package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.persistence.SqlStates;
import com.baytalhikmah.sovereign.service.ListingDesk;
import com.baytalhikmah.sovereign.tenant.RowLevelSecurity;
import com.baytalhikmah.sovereign.tenant.TenantContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RowLevelSecurityTest {

    @Autowired
    RowLevelSecurity rowLevelSecurity;

    @Autowired
    ListingDesk listingDesk;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void twoGuardsKeepSharedRowsApart() {
        rowLevelSecurity.install();

        TenantContext.runAs("nile", () -> {
            listingDesk.list("Ceramic Kettle", Money.of("45.00", "GBP"));
            listingDesk.list("Clay Mug", Money.of("12.00", "GBP"));
        });
        UUID atlasTray = TenantContext.callAs("atlas",
            () -> listingDesk.list("Oak Tray", Money.of("30.00", "GBP")));

        List<String> nileTitles = TenantContext.callAs("nile", listingDesk::titles);
        List<String> atlasTitles = TenantContext.callAs("atlas", listingDesk::titles);
        String nileLoadsAtlasTray = TenantContext.callAs("nile", () -> listingDesk.titleOf(atlasTray));
        long nilePlainSqlCount = TenantContext.callAs("nile", listingDesk::countWithPlainSql);
        Throwable forged = catchThrowable(() -> TenantContext.runAs("nile",
            () -> listingDesk.listFor("atlas", "Fake Tray", Money.of("1.00", "GBP"))));
        Integer allRowsForPlatform = jdbcTemplate.queryForObject(
            "select count(*) from sov_marketplace_listing", Integer.class);

        System.out.println("=== FILTER AND ROW-LEVEL SECURITY ===");
        System.out.println("nile JPQL           : " + nileTitles);
        System.out.println("atlas JPQL          : " + atlasTitles);
        System.out.println("nile loads atlas ID : " + nileLoadsAtlasTray);
        System.out.println("nile plain SQL count: " + nilePlainSqlCount);
        System.out.println("nile forges atlas   : SQLSTATE " + SqlStates.of(forged));
        System.out.println("platform sees       : " + allRowsForPlatform + " rows");

        assertThat(nileTitles).containsExactly("Ceramic Kettle", "Clay Mug");
        assertThat(atlasTitles).containsExactly("Oak Tray");
        assertThat(nileLoadsAtlasTray).isNull();
        assertThat(nilePlainSqlCount).isEqualTo(2);
        assertThat(SqlStates.of(forged)).isEqualTo("42501");
        assertThat(allRowsForPlatform).isEqualTo(3);
    }

}
