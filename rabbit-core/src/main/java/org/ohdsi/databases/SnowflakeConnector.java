package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.utils.URLEncodedUtils;
import org.ohdsi.utilities.files.IniFile;


import static org.ohdsi.databases.SnowflakeConnector.ServerConfig.buildUrl;

/*
 * SnowflakeDB implements all Snowflake specific logic required to connect to, and query, a Snowflake instance.
 *
 * It is implemented as a Singleton, using the enum pattern es described here: https://www.baeldung.com/java-singleton
 */
public enum SnowflakeConnector implements DBConnectorInterface {
    INSTANCE();

    private DbSettings dbSettings = null;
    private ServerConfig serverConfig = null;
    private Connection snowflakeConnection = null;
    private String warehouse;
    private String database;
    private String schema;

    private final DbType dbType = DbType.SNOWFLAKE;
    public final static String ERROR_NO_FIELD_OF_TYPE = "No value was specified for type";
    public final static String ERROR_INVALID_SERVER_STRING = "Server string is not valid";
    public final static String ERROR_INCORRECT_SCHEMA_SPECIFICATION =
            "Database should be specified as 'warehouse.database.schema', " +
                    "e.g. 'computewh.snowflake_sample_data.weather";
    public final static String ERROR_CONNECTION_NOT_INITIALIZED =
            "Snowflake Database connections has not been initialized.";
    private SnowflakeConnector(ServerConfig serverConfig, Connection snowflakeConnection) {
        this.serverConfig = serverConfig;
        this.snowflakeConnection = snowflakeConnection;
    }

    SnowflakeConnector() {
    }

    public void resetConnection() throws SQLException {
        if (this.snowflakeConnection != null) {
            this.snowflakeConnection.close();
        }
        this.snowflakeConnection = null;
    }

    public DBConnectorInterface getInstance(IniFile iniFile) {
        warehouse = iniFile.getOrFail("SNOWFLAKE_WAREHOUSE");
        database = iniFile.getOrFail("SNOWFLAKE_DATABASE");
        schema = iniFile.getOrFail("SNOWFLAKE_SCHEMA");
        dbSettings = new DbSettings();
        dbSettings.server = String.format("https://%s.snowflakecomputing.com", iniFile.getOrFail("SNOWFLAKE_ACCOUNT"));
        dbSettings.database = String.format("%s.%s.%s", warehouse, database, schema);
        dbSettings.domain = dbSettings.database;
        dbSettings.user = iniFile.getOrFail("SNOWFLAKE_USER");
        dbSettings.password = iniFile.getOrFail("SNOWFLAKE_PASSWORD");
        dbSettings.dbType = DbType.SNOWFLAKE;
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;

        return getInstance(dbSettings.server, dbSettings.database, dbSettings.user, dbSettings.password);
    }

    @Override
    public DBConnectorInterface getInstance(String server, String fullSchemaPath, String user, String password) {
        if (this.serverConfig == null) {
            serverConfig = new ServerConfig(server, fullSchemaPath, user, password);
        }
        if (snowflakeConnection == null) {
            snowflakeConnection =  connectToSnowflake(server, fullSchemaPath, user, password);
        }

        return INSTANCE;
    }

    @Override
    public DBConnectorInterface getInstance() {
        if (this.snowflakeConnection == null) {
            throw new RuntimeException(ERROR_CONNECTION_NOT_INITIALIZED);
        }

        return INSTANCE;
    }

    public Connection getConnection() {
        this.checkInitialised();
        return this.snowflakeConnection;
    }

    @Override
    public String getTableSizeQuery(String tableName) {
        return "SELECT COUNT(*) FROM " + serverConfig.database + "." + serverConfig.schema + "." + tableName + ";";
    }

    @Override
    public int getNameIndex() {
        return 1;
    }

    public String getTablesQuery(String database) {
        return String.format("SELECT TABLE_NAME FROM %s.INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '%s'", this.database.toUpperCase(), schema.toUpperCase());
    }

    @Override
    public void checkInitialised() {
        if (this.snowflakeConnection == null) {
            throw new RuntimeException("Snowflake DB/connection was not initialized");
        }
    }

    public Connection getConnection(DbSettings dbSettings) {
        return getConnection(dbSettings.server, dbSettings.database, dbSettings.user, dbSettings.password);
    }

    public Connection getConnection(String server, String fullSchemaPath, String user, String password) {
        SnowflakeConnector.INSTANCE.getInstance(server, fullSchemaPath, user, password);
        return snowflakeConnection;
    }
    public Connection connect(DbSettings dbSettings) throws RuntimeException {
        SnowflakeConnector.INSTANCE.getInstance(dbSettings.server, dbSettings.database, dbSettings.user, dbSettings.password);
        return this.snowflakeConnection;
    }

    public DbSettings getDbSettings() {
        if (dbSettings == null) {
            throw new RuntimeException(String.format("dbSettings were never initialized for class %s", this.getClass().getName()));
        }

        return dbSettings;
    }

    private static Connection connectToSnowflake(String server, String schema, String user, String password) {
        try {
            Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Cannot find JDBC driver. Make sure the file snowflake-jdbc-x.xx.xx.jar is in the path: " + ex.getMessage());
        }
        String url = buildUrl(server, schema, user, password);
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException ex) {
            throw new RuntimeException("Cannot connect to Snowflake server: " + ex.getMessage());
        }
    }

    public ResultSet getFieldNames(String table) {
        try {
            DatabaseMetaData metadata = this.snowflakeConnection.getMetaData();
            return metadata.getColumns(null, null, table, null);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static class ServerConfig {
        public final String server;
        public final String user;
        public final String password;
        public final String fullSchemaPath;
        public final String warehouse;
        public final String database;
        public final String schema;

        public static List<String> checkSnowflakeConfig(String server, String user, String password, String schemaName)
                throws RuntimeException {
            List<String> errors = new ArrayList<>();

            if (StringUtils.isEmpty(schemaName)) {
                errors.add(ERROR_NO_FIELD_OF_TYPE + " database");
            }
            else {
                String[] parts = schemaName.split("\\.");
                if (parts.length != 3) {
                    errors.add(ERROR_INCORRECT_SCHEMA_SPECIFICATION);
                }
            }
            if (StringUtils.isEmpty(user)) {
                errors.add(ERROR_NO_FIELD_OF_TYPE + " user");
            }
            if (StringUtils.isEmpty(password)) {
                errors.add(ERROR_NO_FIELD_OF_TYPE + " password");
            }

            try {
                URLEncodedUtils.parse(new URI(server), String.valueOf(StandardCharsets.UTF_8));
            } catch (URISyntaxException ex) {
                errors.add(String.format(ERROR_INVALID_SERVER_STRING + " (%s)", ex.getMessage()));
            }

            return errors;
        }

        public static String buildUrl(String server, String schema, String user, String password) {
            final String jdbcPrefix = "jdbc:snowflake://";
            String url = (!server.startsWith(jdbcPrefix) ? jdbcPrefix : "") + server;
            if (!url.contains("?")) {
                url += "?";
            }

            String[] parts = splitDatabaseName(schema);
            url = appendParameterIfSet(url, "warehouse", parts[0]);
            url = appendParameterIfSet(url, "db", parts[1]);
            url = appendParameterIfSet(url, "schema", parts[2]);
            url = appendParameterIfSet(url, "user", user);
            url = appendParameterIfSet(url, "password", password);

            return url;
        }

        static public String[] splitDatabaseName(String databaseName) {
            String[] parts = databaseName.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException(ERROR_INCORRECT_SCHEMA_SPECIFICATION);
            }

            return parts;
        }

        private static String appendParameterIfSet(String url, String name, String value) {
            if (!StringUtils.isEmpty(value)) {
                return String.format("%s%s%s=%s", url, (url.endsWith("?") ? "" : "&"), name, value);
            }
            else {
                throw new RuntimeException(String.format(ERROR_NO_FIELD_OF_TYPE + " %s", name));
            }
        }
        public ServerConfig(String server, String fullSchemaPath, String user, String password) {
            this.server = server;
            this.fullSchemaPath = fullSchemaPath;
            this.user = user;
            this.password = password;
            String parts[] = splitDatabaseName(fullSchemaPath);
            this.warehouse = parts[0];
            this.database = parts[1];
            this.schema = parts[2];
        }
    }
}
