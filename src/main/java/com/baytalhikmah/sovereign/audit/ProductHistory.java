package com.baytalhikmah.sovereign.audit;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;

/**
 * Reads the history of products back as entities: the full timeline with changed fields, the state at a revision,
 * and the state at any moment in time. Historical entities are detached copies; the current data is never touched.
 */
@Service
@Transactional(readOnly = true)
public class ProductHistory {

    private final EntityManager entityManager;

    public ProductHistory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ProductVersion> timeline(UUID productId) {
        List<?> rows = reader().createQuery()
            .forRevisionsOfEntityWithChanges(Product.class, true)
            .add(AuditEntity.id().eq(productId))
            .addOrder(AuditEntity.revisionNumber().asc())
            .getResultList();
        return rows.stream().map(row -> toVersion((Object[]) row)).toList();
    }

    public Optional<Product> atRevision(UUID productId, long revision) {
        return Optional.ofNullable(reader().find(Product.class, productId, revision));
    }

    public Optional<Product> asOf(UUID productId, Instant moment) {
        AuditReader reader = reader();
        Number revision;
        try {
            revision = reader.getRevisionNumberForDate(Date.from(moment));
        }
        catch (RevisionDoesNotExistException beforeFirstRevision) {
            return Optional.empty();
        }
        return Optional.ofNullable(reader.find(Product.class, productId, revision));
    }

    private AuditReader reader() {
        return AuditReaderFactory.get(entityManager);
    }

    private static ProductVersion toVersion(Object[] row) {
        Product state = (Product) row[0];
        AuditRevision revision = (AuditRevision) row[1];
        RevisionType type = (RevisionType) row[2];
        Set<String> changed = new TreeSet<>();
        for (Object field : (Set<?>) row[3]) {
            changed.add(field.toString());
        }
        return new ProductVersion(revision.getId(), revision.getChangedAt(), revision.getUsername(), type.name(),
                state.getName(), state.getPrice().toString(), changed);
    }

}
