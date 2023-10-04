package org.ohdsi.databases;

import org.ohdsi.utilities.files.Row;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class DBRowIterator implements Iterator<Row> {

    private ResultSet resultSet;

    private boolean hasNext;

    private Set<String> columnNames = new HashSet<>();

    public DBRowIterator(String sql, RichConnection richConnection) {
        Statement statement;
        try {
            sql.trim();
            if (sql.endsWith(";"))
                sql = sql.substring(0, sql.length() - 1);
            if (richConnection.isVerbose()) {
                String abbrSQL = sql.replace('\n', ' ').replace('\t', ' ').trim();
                if (abbrSQL.length() > 100)
                    abbrSQL = abbrSQL.substring(0, 100).trim() + "...";
                System.out.println("Executing query: " + abbrSQL);
            }
            long start = System.currentTimeMillis();
            statement = richConnection.getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            resultSet = statement.executeQuery(sql);
            hasNext = resultSet.next();
            if (richConnection.isVerbose())
                richConnection.outputQueryStats(statement, System.currentTimeMillis() - start);
        } catch (SQLException e) {
            System.err.println(sql);
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            resultSet = null;
            hasNext = false;
        }
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Row next() {
        try {
            Row row = new Row();
            ResultSetMetaData metaData;
            metaData = resultSet.getMetaData();
            columnNames.clear();

            for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
                String columnName = metaData.getColumnName(i);
                if (columnNames.add(columnName)) {
                    String value;
                    try {
                        value = resultSet.getString(i);
                    } catch (Exception e) {
                        value = "";
                    }
                    if (value == null)
                        value = "";

                    row.add(columnName, value.replace(" 00:00:00", ""));
                }
            }
            hasNext = resultSet.next();
            if (!hasNext) {
                resultSet.close();
                resultSet = null;
            }
            return row;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
    }
}
