package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.baytalhikmah.sovereign.service.ShopCustomers;
import com.baytalhikmah.sovereign.tenant.TenantContext;
import com.baytalhikmah.sovereign.tenant.TenantProvisioner;
import com.baytalhikmah.sovereign.tenant.TenantTokenFilter;
import com.baytalhikmah.sovereign.tenant.TenantTokens;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("tenancy")
@SpringBootTest
class TenantTokenTest {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    TenantTokenFilter tenantTokenFilter;

    @Autowired
    TenantTokens tenantTokens;

    @Autowired
    TenantProvisioner provisioner;

    @Autowired
    ShopCustomers shopCustomers;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilters(tenantTokenFilter).build();
    }

    @Test
    void theTokenDecidesTheTenantOfEveryRequest() throws Exception {
        provisioner.provision("cedar");
        provisioner.provision("delta");
        TenantContext.runAs("cedar", () -> {
            shopCustomers.register("Hana Saleh", "hana@cedar.tenant.test");
            shopCustomers.register("Idris Noor", "idris@cedar.tenant.test");
        });
        TenantContext.runAs("delta", () -> shopCustomers.register("Karim Aziz", "karim@delta.tenant.test"));

        String cedarToken = tenantTokens.issue("hana", "cedar", Duration.ofMinutes(15));
        String deltaToken = tenantTokens.issue("karim", "delta", Duration.ofMinutes(15));
        String expiredToken = tenantTokens.issue("hana", "cedar", Duration.ofMinutes(-1));
        String foreignToken = new TenantTokens("a-completely-different-secret-of-32-bytes!").issue("eve", "cedar",
            Duration.ofMinutes(15));
        String badTenantToken = tenantTokens.issue("eve", "Cedar; drop", Duration.ofMinutes(15));

        MockHttpServletResponse cedar = call(get("/api/customers"), cedarToken);
        MockHttpServletResponse delta = call(get("/api/customers"), deltaToken);
        MockHttpServletResponse background = call(get("/api/customers/background"), cedarToken);
        MockHttpServletResponse noToken = mockMvc.perform(get("/api/customers")).andReturn().getResponse();
        MockHttpServletResponse expired = call(get("/api/customers"), expiredToken);
        MockHttpServletResponse foreign = call(get("/api/customers"), foreignToken);
        MockHttpServletResponse badTenant = call(get("/api/customers"), badTenantToken);

        System.out.println("=== TENANT FROM THE TOKEN ===");
        System.out.println("cedar token      : " + cedar.getStatus() + " " + cedar.getContentAsString());
        System.out.println("delta token      : " + delta.getStatus() + " " + delta.getContentAsString());
        System.out.println("background       : " + background.getStatus() + " " + background.getContentAsString());
        System.out.println("no token         : " + noToken.getStatus());
        System.out.println("expired token    : " + expired.getStatus());
        System.out.println("foreign signature: " + foreign.getStatus());
        System.out.println("bad tenant claim : " + badTenant.getStatus());
        System.out.println("test thread after: " + TenantContext.get());

        assertThat(cedar.getStatus()).isEqualTo(200);
        assertThat(cedar.getContentAsString()).isEqualTo("[\"Hana Saleh\",\"Idris Noor\"]");
        assertThat(delta.getContentAsString()).isEqualTo("[\"Karim Aziz\"]");
        assertThat(background.getContentAsString())
            .contains("\"worker\":\"task-")
            .contains("\"tenant\":\"cedar\"")
            .contains("\"customers\":[\"Hana Saleh\",\"Idris Noor\"]");
        assertThat(noToken.getStatus()).isEqualTo(401);
        assertThat(expired.getStatus()).isEqualTo(401);
        assertThat(foreign.getStatus()).isEqualTo(401);
        assertThat(badTenant.getStatus()).isEqualTo(401);
        assertThat(TenantContext.get()).isEqualTo(TenantContext.PLATFORM);
    }

    private MockHttpServletResponse call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token)).andReturn().getResponse();
    }

}
