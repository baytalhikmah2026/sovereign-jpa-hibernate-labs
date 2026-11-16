package com.baytalhikmah.sovereign.config;

import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

import jakarta.persistence.EntityManagerFactory;

/** With the profile metrics, Hibernate's statistics become Micrometer meters tagged entityManagerFactory=sovereign. */
@Configuration(proxyBeanMethods = false)
@Profile("metrics")
public class HibernateMetricsConfig {

    public static final String FACTORY_NAME = "sovereign";

    @Bean
    MeterBinder hibernateMetrics(EntityManagerFactory entityManagerFactory) {
        return new HibernateMetrics(entityManagerFactory.unwrap(SessionFactory.class), FACTORY_NAME, Tags.empty());
    }

}
