package com.baytalhikmah.sovereign.persistence;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/** The shared implementation behind every repository: Spring Data's SimpleJpaRepository plus the shop's extras. */
@Transactional(readOnly = true)
public class SovereignRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements SovereignRepository<T, ID> {

    private final JpaEntityInformation<T, ?> entityInformation;

    private final EntityManager entityManager;

    public SovereignRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    public T findRequired(ID id) {
        return findById(id).orElseThrow(() -> new RecordNotFoundException(entityInformation.getEntityName(), id));
    }

    @Override
    public void detach(T entity) {
        entityManager.detach(entity);
    }

}
