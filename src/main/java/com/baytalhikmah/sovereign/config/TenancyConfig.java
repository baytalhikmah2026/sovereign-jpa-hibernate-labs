package com.baytalhikmah.sovereign.config;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskDecorator;

import com.baytalhikmah.sovereign.tenant.DatabaseRouter;
import com.baytalhikmah.sovereign.tenant.ShopSchemas;
import com.baytalhikmah.sovereign.tenant.TenantIdentifierResolver;
import com.baytalhikmah.sovereign.tenant.TenantTaskDecorator;

/**
 * With the tenancy profile, every Hibernate session works for the current tenant:
 * in the tenant's schema (sovereign.tenancy.mode=schema, the default) or in its own database (=database).
 */
@Configuration(proxyBeanMethods = false)
@Profile("tenancy")
public class TenancyConfig {

    @Bean
    @ConditionalOnProperty(name = "sovereign.tenancy.mode", havingValue = "schema", matchIfMissing = true)
    HibernatePropertiesCustomizer schemaPerTenant() {
        return properties -> {
            properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdentifierResolver());
            properties.put(MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER, new ShopSchemas());
        };
    }

    @Bean
    @ConditionalOnProperty(name = "sovereign.tenancy.mode", havingValue = "database")
    HibernatePropertiesCustomizer databasePerTenant(DatabaseRouter databaseRouter) {
        return properties -> {
            properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdentifierResolver());
            properties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, databaseRouter);
        };
    }

    /** Spring Boot applies a single TaskDecorator bean to its application task executor. */
    @Bean
    TaskDecorator tenantTaskDecorator() {
        return new TenantTaskDecorator();
    }

}
