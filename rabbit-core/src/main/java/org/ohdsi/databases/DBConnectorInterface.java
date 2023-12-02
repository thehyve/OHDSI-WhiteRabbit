package org.ohdsi.databases;

import org.ohdsi.databases.configuration.DBConfiguration;
import org.ohdsi.databases.configuration.DbSettings;
import org.ohdsi.databases.configuration.DbType;
import org.ohdsi.utilities.files.IniFile;
import org.ohdsi.utilities.files.Row;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface DBConnectorInterface {

    DBConnection getDBConnection();

    DbType getDbType();

    String getTableSizeQuery(String tableName);

    void checkInitialised();

    DBConnectorInterface getInstance();

    DBConnectorInterface getInstance(String server, String database, String user, String password);
    /**
     * Returns the row count of the specified table.
     *
     * @param tableName name of table
     * @return size of table in rows
     */
    default long getTableSize(String tableName ) {
        long returnVal;
        QueryResult qr = SQLUtils.query(getTableSizeQuery(tableName), getDBConnection());
        try {
            returnVal = Long.parseLong(qr.iterator().next().getCells().get(0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            qr.close();
        }
        return returnVal;
    }

    default void use(String ignoredDatabase) {}

    default void close() {
        // no-op by default, so singletons don't need to implement it
    }

    String getDatabase();

    default List<String> getTableNames() {
        List<String> names = new ArrayList<>();
        String query = this.getTablesQuery(getDatabase());

		for (Row row : SQLUtils.query(query, new DBConnection(this, getDbType(), false))) {
            names.add(row.getCells().get(0));
        }

        return names;
    }

    default List<FieldInfo> fetchTableStructure(String table, ScanParameters scanParameters) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        ResultSet rs = getFieldNamesFromJDBC(table);
        try {
            while (rs.next()) {
                FieldInfo fieldInfo = new FieldInfo(scanParameters, rs.getString("COLUMN_NAME"));
                fieldInfo.type = rs.getString("TYPE_NAME");
                fieldInfo.rowCount = getTableSize(table);
                fieldInfos.add(fieldInfo);
            }
        } catch (
                SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
        return fieldInfos;
    }

    default ResultSet getFieldNamesFromJDBC(String table) {
        try {
            DatabaseMetaData metadata = getDBConnection().getMetaData();
            return metadata.getColumns(null, null, table, null);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    String getTablesQuery(String database);

    String getRowSampleQuery(String table, long rowCount, long sampleSize);

    default DbSettings getDbSettings() {
        return getDBConfiguration().toDbSettings();
    }

    default DbSettings getDbSettings(IniFile iniFile) {
        getDBConfiguration().loadAndValidateConfiguration(iniFile);
        return getDBConfiguration().toDbSettings();
    }

    DBConfiguration getDBConfiguration();
}
