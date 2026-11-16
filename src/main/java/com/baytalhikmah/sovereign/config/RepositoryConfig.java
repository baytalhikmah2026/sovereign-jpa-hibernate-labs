package com.baytalhikmah.sovereign.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.baytalhikmah.sovereign.persistence.SovereignRepositoryFactoryBean;

/** Spring Data JPA repositories live in the persistence package and are all built by the shop's own factory. */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackages = "com.baytalhikmah.sovereign.persistence",
        repositoryFactoryBeanClass = SovereignRepositoryFactoryBean.class)
public class RepositoryConfig {
}
