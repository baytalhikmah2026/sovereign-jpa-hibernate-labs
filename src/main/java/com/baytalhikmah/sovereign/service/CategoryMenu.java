package com.baytalhikmah.sovereign.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.ProductCategory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** The shop's category menu: every top-level category with all its sub-categories, indented by level. */
@Service
public class CategoryMenu {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<String> menu(String slugPrefix) {
        List<ProductCategory> roots = entityManager.createQuery("""
                select c from ProductCategory c
                where c.parent is null and c.slug like :prefix
                order by c.slug""", ProductCategory.class)
            .setParameter("prefix", slugPrefix + "%")
            .getResultList();
        List<String> lines = new ArrayList<>();
        roots.forEach(root -> addWithChildren(root, 0, lines));
        return lines;
    }

    private void addWithChildren(ProductCategory category, int level, List<String> lines) {
        lines.add("  ".repeat(level) + category.getName());
        category.getChildren().forEach(child -> addWithChildren(child, level + 1, lines));
    }

}
