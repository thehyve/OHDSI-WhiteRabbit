package org.ohdsi.databases;

import org.ohdsi.utilities.files.Row;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueryResult implements Iterable<Row> {
    private String sql;

    private List<DBRowIterator> iterators = new ArrayList<>();
    private RichConnection connectionInterface;

    public QueryResult(String sql, RichConnection connectionInterface) {
        this.sql = sql;
        this.connectionInterface = connectionInterface;
    }

    @Override
    public Iterator<Row> iterator() {
        DBRowIterator iterator = new DBRowIterator(sql, connectionInterface);
        iterators.add(iterator);
        return iterator;
    }

    public void close() {
        for (DBRowIterator iterator : iterators) {
            iterator.close();
        }
    }
}
