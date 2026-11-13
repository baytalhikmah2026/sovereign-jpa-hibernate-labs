package com.baytalhikmah.sovereign.config;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.baytalhikmah.sovereign.tenant.ShopSchemas;
import com.baytalhikmah.sovereign.tenant.TenantIdentifierResolver;

/** With the tenancy profile, every Hibernate session works in the schema of the current tenant. */
@Configuration(proxyBeanMethods = false)
@Profile("tenancy")
public class TenancyConfig {

    @Bean
    HibernatePropertiesCustomizer schemaPerTenant() {
        return properties -> {
            properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdentifierResolver());
            properties.put(MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER, new ShopSchemas());
        };
    }

}
