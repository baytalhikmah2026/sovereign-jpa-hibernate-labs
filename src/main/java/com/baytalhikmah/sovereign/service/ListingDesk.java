package com.baytalhikmah.sovereign.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.MarketplaceListing;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.tenant.RowLevelSecurity;
import com.baytalhikmah.sovereign.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** The marketplace desk of the current shop. Every transaction starts by entering the shop's tenant. */
@Repository
public class ListingDesk {

    private final RowLevelSecurity rowLevelSecurity;

    @PersistenceContext
    private EntityManager entityManager;

    public ListingDesk(RowLevelSecurity rowLevelSecurity) {
        this.rowLevelSecurity = rowLevelSecurity;
    }

    @Transactional
    public UUID list(String title, Money price) {
        return listFor(TenantContext.get(), title, price);
    }

    /** Lists a product under any tenant name: what a bug or an attacker might try. */
    @Transactional
    public UUID listFor(String tenantId, String title, Money price) {
        rowLevelSecurity.enterTenant(entityManager);
        MarketplaceListing listing = new MarketplaceListing(tenantId, title, price);
        entityManager.persist(listing);
        entityManager.flush();
        return listing.getId();
    }

    /** A JPQL query: the Hibernate filter adds the tenant condition. */
    @Transactional(readOnly = true)
    public List<String> titles() {
        rowLevelSecurity.enterTenant(entityManager);
        return entityManager.createQuery("select l.title from MarketplaceListing l order by l.title", String.class)
            .getResultList();
    }

    /** Loading by ID: Hibernate filters do not apply here, only the database policy does. */
    @Transactional(readOnly = true)
    public String titleOf(UUID listingId) {
        rowLevelSecurity.enterTenant(entityManager);
        MarketplaceListing listing = entityManager.find(MarketplaceListing.class, listingId);
        return listing == null ? null : listing.getTitle();
    }

    /** Plain SQL that forgets the tenant condition: Hibernate filters do not apply to it either. */
    @Transactional(readOnly = true)
    public long countWithPlainSql() {
        rowLevelSecurity.enterTenant(entityManager);
        return ((Number) entityManager.createNativeQuery("select count(*) from sov_marketplace_listing")
            .getSingleResult()).longValue();
    }

}
