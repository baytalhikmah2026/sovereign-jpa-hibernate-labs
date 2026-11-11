package com.baytalhikmah.sovereign.persistence;

import java.sql.SQLException;

/** Finds the five-character SQLSTATE code that the database attached to an error, however deeply it is wrapped. */
public final class SqlStates {

    private SqlStates() {
    }

    public static String of(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlError && sqlError.getSQLState() != null) {
                return sqlError.getSQLState();
            }
        }
        return "none";
    }

}
