package com.baytalhikmah.sovereign.config;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.SessionEventSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.baytalhikmah.sovereign.audit.AuditStampInterceptor;
import com.baytalhikmah.sovereign.audit.SqlTraceInspector;

/** Registers the global audit interceptor, and the SQL trace comments when the sql-trace profile is active. */
@Configuration(proxyBeanMethods = false)
public class AuditConfig {

    @Bean
    HibernatePropertiesCustomizer auditStampInterceptor() {
        return properties -> properties.put(SessionEventSettings.INTERCEPTOR, new AuditStampInterceptor());
    }

    @Bean
    @Profile("sql-trace")
    HibernatePropertiesCustomizer sqlTraceInspector() {
        return properties -> properties.put(JdbcSettings.STATEMENT_INSPECTOR, new SqlTraceInspector());
    }

}
