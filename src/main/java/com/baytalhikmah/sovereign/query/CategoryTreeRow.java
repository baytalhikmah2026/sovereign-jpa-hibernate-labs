package com.baytalhikmah.sovereign.query;

import java.util.UUID;

import com.blazebit.persistence.CTE;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** The shape of one row of the recursive category walk: a "virtual" entity that only exists inside a query. */
@CTE
@Entity
public class CategoryTreeRow {

    @Id
    private UUID id;

    private String slug;

    private int depth;

    protected CategoryTreeRow() {
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public int getDepth() {
        return depth;
    }

}
