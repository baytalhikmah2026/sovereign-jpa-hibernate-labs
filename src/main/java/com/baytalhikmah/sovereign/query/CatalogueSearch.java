package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.hibernate.search.mapper.orm.Search;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/** Full-text search over the product catalogue, answered by the sharded Lucene index "products". */
@Repository
@Profile("search")
public class CatalogueSearch {

    private static final int MAX_HITS = 20;

    @PersistenceContext
    private EntityManager entityManager;

    private final EntityManagerFactory entityManagerFactory;

    public CatalogueSearch(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /** Products whose name contains the words, searched on every shard, ordered by SKU. */
    @Transactional(readOnly = true)
    public List<String> findNames(String words) {
        return Search.session(entityManager).search(Product.class)
            .where(f -> f.match().field("name").matching(words))
            .sort(f -> f.field("sku"))
            .fetchHits(MAX_HITS)
            .stream().map(Product::getName).toList();
    }

    /** The same search inside one department: only the department's shard is opened. */
    @Transactional(readOnly = true)
    public List<String> findNamesInDepartment(String department, String words) {
        return Search.session(entityManager).search(Product.class)
            .where(f -> f.bool()
                .must(f.match().field("name").matching(words))
                .filter(f.match().field("department").matching(department)))
            .routing(department)
            .sort(f -> f.field("sku"))
            .fetchHits(MAX_HITS)
            .stream().map(Product::getName).toList();
    }

    /** How many documents the shard holding this department contains (other departments included). */
    @Transactional(readOnly = true)
    public long countInShardOf(String department) {
        return Search.session(entityManager).search(Product.class)
            .where(f -> f.matchAll())
            .routing(department)
            .fetchTotalHitCount();
    }

    /** Rebuilds the whole index from the database (needed after changes made without Hibernate). */
    public void rebuildIndex() throws InterruptedException {
        Search.mapping(entityManagerFactory).scope(Product.class).massIndexer().startAndWait();
    }

}
