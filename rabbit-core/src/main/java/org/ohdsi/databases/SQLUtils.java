package org.ohdsi.databases;

public class SQLUtils {
    /**
     * Query the database using the provided SQL statement.
     *
     * @param sql
     * @return
     */
    public static QueryResult query(String sql, DBConnection connection) {
        return new QueryResult(sql, connection, false);
    }
}
