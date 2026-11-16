package com.baytalhikmah.sovereign.persistence;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/**
 * Keeps Hibernate's schema tool (ddl-auto) away from the schema "compliance": Flyway owns it in every environment.
 * Hibernate may still read and write its tables; it never creates, changes or drops them.
 */
public class ComplianceSchemaFilter implements SchemaFilterProvider {

    public static final String FLYWAY_SCHEMA = "compliance";

    private static final SchemaFilter SKIP_FLYWAY_SCHEMA = new SchemaFilter() {

        @Override
        public boolean includeNamespace(Namespace namespace) {
            Identifier schema = namespace.getPhysicalName().schema();
            return schema == null || !schema.getText().equalsIgnoreCase(FLYWAY_SCHEMA);
        }

        @Override
        public boolean includeTable(Table table) {
            return true;
        }

        @Override
        public boolean includeSequence(Sequence sequence) {
            return true;
        }

    };

    @Override
    public SchemaFilter getCreateFilter() {
        return SKIP_FLYWAY_SCHEMA;
    }

    @Override
    public SchemaFilter getDropFilter() {
        return SKIP_FLYWAY_SCHEMA;
    }

    @Override
    public SchemaFilter getTruncatorFilter() {
        return SKIP_FLYWAY_SCHEMA;
    }

    @Override
    public SchemaFilter getMigrateFilter() {
        return SKIP_FLYWAY_SCHEMA;
    }

    @Override
    public SchemaFilter getValidateFilter() {
        return SchemaFilter.ALL;
    }

}
