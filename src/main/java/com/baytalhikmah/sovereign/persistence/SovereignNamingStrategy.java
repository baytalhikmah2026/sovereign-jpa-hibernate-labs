package com.baytalhikmah.sovereign.persistence;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class SovereignNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl {

    private static final String TABLE_PREFIX = "sov_";

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment environment) {
        return withPrefix(super.toPhysicalTableName(logicalName, environment));
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment environment) {
        return withPrefix(super.toPhysicalSequenceName(logicalName, environment));
    }

    private Identifier withPrefix(Identifier name) {
        if (name == null || name.getText().startsWith(TABLE_PREFIX)) {
            return name;
        }
        return Identifier.toIdentifier(TABLE_PREFIX + name.getText(), name.isQuoted());
    }

}
