package com.baytalhikmah.sovereign.query;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Asks PostgreSQL how it would run a statement. A developer tool: only ever pass SQL written in this project. */
@Repository
public class PlanInspector {

    private final JdbcTemplate jdbc;

    public PlanInspector(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The plan PostgreSQL chooses on its own. The statement is not executed. */
    public List<String> explain(String sql, Object... parameters) {
        return jdbc.queryForList("explain (costs off) " + sql, String.class, parameters);
    }

    /** The plan when sequential scans are discouraged, which shows whether a usable index exists. */
    @Transactional
    public List<String> explainPreferringIndexes(String sql, Object... parameters) {
        jdbc.execute("set local enable_seqscan = off");
        return explain(sql, parameters);
    }

}
