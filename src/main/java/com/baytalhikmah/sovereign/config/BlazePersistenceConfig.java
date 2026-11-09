package com.baytalhikmah.sovereign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;

import jakarta.persistence.EntityManagerFactory;

@Configuration(proxyBeanMethods = false)
public class BlazePersistenceConfig {

    /** One factory for the whole application, built on top of Hibernate's EntityManagerFactory. */
    @Bean
    CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory entityManagerFactory) {
        return Criteria.getDefault().createCriteriaBuilderFactory(entityManagerFactory);
    }

}
