package com.baytalhikmah.sovereign.persistence;

import java.time.Duration;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import jakarta.persistence.EntityManager;

/**
 * Builds every repository of the shop. Two things are the same for all of them:
 * the base class (SovereignRepositoryImpl) and a timer around each call (RepositoryCallLog).
 */
public class SovereignRepositoryFactoryBean<R extends JpaRepository<T, ID>, T, ID>
        extends JpaRepositoryFactoryBean<R, T, ID> {

    private BeanFactory beanFactory;

    public SovereignRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        RepositoryCallLog callLog = beanFactory.getBean(RepositoryCallLog.class);
        JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager) {
            @Override
            protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
                return SovereignRepositoryImpl.class;
            }
        };
        factory.addRepositoryProxyPostProcessor((proxyFactory, repositoryInformation) -> {
            String repositoryName = repositoryInformation.getRepositoryInterface().getSimpleName();
            proxyFactory.addAdvice((MethodInterceptor) invocation -> {
                long start = System.nanoTime();
                try {
                    return invocation.proceed();
                }
                finally {
                    callLog.record(repositoryName + "." + invocation.getMethod().getName(),
                        Duration.ofNanos(System.nanoTime() - start));
                }
            });
        });
        return factory;
    }

}
