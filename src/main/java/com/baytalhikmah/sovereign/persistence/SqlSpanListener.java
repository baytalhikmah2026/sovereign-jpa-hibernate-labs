package com.baytalhikmah.sovereign.persistence;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * Turns every SQL statement that passes through P6Spy into an OpenTelemetry span, a child of whatever span is
 * current on the calling thread. The span shows the SQL with "?" placeholders, never the values.
 */
public class SqlSpanListener extends SimpleJdbcEventListener {

    static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system.name");

    static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation.name");

    static final AttributeKey<String> DB_TABLE = AttributeKey.stringKey("db.collection.name");

    static final AttributeKey<String> DB_QUERY = AttributeKey.stringKey("db.query.text");

    private static final Pattern TABLE = Pattern.compile("\\b(?:from|into|update)\\s+([a-z0-9_.]+)");

    private final Tracer tracer;

    public SqlSpanListener(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statement, long timeElapsedNanos, SQLException error) {
        String sql = statement.getSql();
        if (sql == null || sql.isBlank()) {
            return;
        }
        String lower = sql.strip().toLowerCase(Locale.ROOT);
        String operation = lower.split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        Matcher table = TABLE.matcher(lower);
        String tableName = table.find() ? table.group(1) : "";

        Instant end = Instant.now();
        Span span = tracer.spanBuilder(tableName.isEmpty() ? operation : operation + " " + tableName)
            .setSpanKind(SpanKind.CLIENT)
            .setStartTimestamp(end.minusNanos(timeElapsedNanos))
            .setAttribute(DB_SYSTEM, "postgresql")
            .setAttribute(DB_OPERATION, operation)
            .setAttribute(DB_TABLE, tableName)
            .setAttribute(DB_QUERY, sql)
            .startSpan();
        if (error != null) {
            span.setStatus(StatusCode.ERROR, error.getSQLState());
            span.recordException(error);
        }
        span.end(end);
    }

}
