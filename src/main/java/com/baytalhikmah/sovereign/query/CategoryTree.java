package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baytalhikmah.sovereign.domain.ProductCategory;
import com.blazebit.persistence.CriteriaBuilderFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class CategoryTree {

    @PersistenceContext
    private EntityManager entityManager;

    private final CriteriaBuilderFactory criteriaBuilderFactory;

    public CategoryTree(CriteriaBuilderFactory criteriaBuilderFactory) {
        this.criteriaBuilderFactory = criteriaBuilderFactory;
    }

    /** The category with this slug and every category below it, at any depth, in one SQL statement. */
    public List<CategoryTreeRow> subtree(String rootSlug) {
        return criteriaBuilderFactory.create(entityManager, CategoryTreeRow.class, "row")
            .withRecursive(CategoryTreeRow.class)
                .from(ProductCategory.class, "category")
                .bind("id").select("category.id")
                .bind("slug").select("category.slug")
                .bind("depth").select("0")
                .where("category.slug").eq(rootSlug)
            .unionAll()
                .from(ProductCategory.class, "child")
                .from(CategoryTreeRow.class, "parentRow")
                .bind("id").select("child.id")
                .bind("slug").select("child.slug")
                .bind("depth").select("parentRow.depth + 1")
                .where("child.parent.id").eqExpression("parentRow.id")
            .end()
            .orderByAsc("row.depth")
            .orderByAsc("row.slug")
            .getResultList();
    }

}
