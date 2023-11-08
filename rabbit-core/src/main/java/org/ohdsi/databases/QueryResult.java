package org.ohdsi.databases;

import org.ohdsi.utilities.files.Row;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueryResult implements Iterable<Row> {
    private String sql;

    private List<DBRowIterator> iterators = new ArrayList<>();
    private DBConnection dbConnection;

    public QueryResult(String sql, DBConnection dbConnection, boolean verbose) {
        this.sql = sql;
        this.dbConnection = dbConnection;
    }

    @Override
    public Iterator<Row> iterator() {
        DBRowIterator iterator = new DBRowIterator(sql, dbConnection, false);
        iterators.add(iterator);
        return iterator;
    }

    public void close() {
        for (DBRowIterator iterator : iterators) {
            iterator.close();
        }
    }
}
