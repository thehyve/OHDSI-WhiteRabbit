package org.ohdsi.databases;

import org.ohdsi.databases.configuration.ConfigurationField;
import org.ohdsi.databases.configuration.DBConfiguration;
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
//    default DBConnectorInterface getInstance(DbSettings dbSettings) {
//        return getInstance(dbSettings.server, dbSettings.database, dbSettings.user, dbSettings.password);
//    }
    DBConnectorInterface getInstance(String server, String database, String user, String password);
    /**
     * Returns the row count of the specified table.
     *
     * @param tableName
     * @return
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

    default void use(String database) {
        return;
    }

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

    List<FieldInfo> fetchTableStructure(String table, ScanParameters scanParameters);
    default List<FieldInfo> fetchTableStructureThroughJdbc(String table, ScanParameters scanParameters) {
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


    default int getNameIndex() {
        return 0;
    }
    String getTablesQuery(String database);

    public ResultSet getFieldNames(String table);

    static DBConnectorInterface getDBConnectorInstance(IniFile iniFile) {
        if (iniFile.getDataType().equalsIgnoreCase("snowflake")) {
            return SnowflakeConnector.INSTANCE.getInstance(iniFile);
        }

        return null;
    }

    default DbSettings getDbSettings() {
        return getDBConfiguration().toDbSettings();
    }

    public List<ConfigurationField> getFields();

    public DBConfiguration getDBConfiguration();
    public void setDBConfiguration(DBConfiguration dbConfiguration);
}
