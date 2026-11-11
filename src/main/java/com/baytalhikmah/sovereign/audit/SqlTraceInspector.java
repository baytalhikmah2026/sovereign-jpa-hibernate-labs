package com.baytalhikmah.sovereign.audit;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Puts a comment naming the current actor in front of every SQL statement Hibernate sends. */
public class SqlTraceInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        return "/* sovereign actor=" + safe(CurrentActor.get()) + " */ " + sql;
    }

    /** Only letters, digits, dot, dash and underscore may reach the SQL text; everything else becomes "_". */
    static String safe(String actor) {
        return actor.replaceAll("[^A-Za-z0-9._-]", "_");
    }

}
