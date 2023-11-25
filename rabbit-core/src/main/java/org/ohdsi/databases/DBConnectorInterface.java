package org.ohdsi.databases;

import org.ohdsi.databases.configuration.ConfigurationField;
import org.ohdsi.utilities.files.IniFile;
import org.ohdsi.utilities.files.Row;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public interface DBConnectorInterface {

    Connection getConnection();

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
    default long getTableSize(String tableName, RichConnection connectionInterface) {
        long returnVal;
        QueryResult qr = SQLUtils.query(getTableSizeQuery(tableName), connectionInterface.getConnection());
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

    default List<String> getTableNames(String database, DBConnection connection) {
        List<String> names = new ArrayList<>();
        String query = this.getTablesQuery(database);

		for (Row row : SQLUtils.query(query, connection)) {
            names.add(row.getCells().get(0));
        }

        return names;
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

    DbSettings getDbSettings();

    public List<ConfigurationField> getFields();
}
