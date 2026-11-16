package com.baytalhikmah.sovereign.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * What every repository of the shop can do, on top of Spring Data's JpaRepository.
 * Not a repository itself (@NoRepositoryBean): it is the common base of all of them.
 */
@NoRepositoryBean
public interface SovereignRepository<T, ID> extends JpaRepository<T, ID> {

    /** Like findById, but a missing row is an error that names the entity and the ID. */
    T findRequired(ID id);

    /** Forgets the entity in the current persistence context, so later changes are not saved. */
    void detach(T entity);

}
