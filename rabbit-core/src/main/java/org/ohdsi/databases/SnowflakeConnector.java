package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.ohdsi.databases.configuration.*;
import org.ohdsi.utilities.files.IniFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ohdsi.databases.SnowflakeConnector.SnowFlakeConfiguration.*;

/*
 * SnowflakeDB implements all Snowflake specific logic required to connect to, and query, a Snowflake instance.
 *
 * It is implemented as a Singleton, using the enum pattern es described here: https://www.baeldung.com/java-singleton
 */
public enum SnowflakeConnector implements DBConnectorInterface {
    INSTANCE();

    static Logger logger = LoggerFactory.getLogger(SnowflakeConnector.class);

    private DbSettings dbSettings = null;

    SnowFlakeConfiguration configuration = null;
    private Connection snowflakeConnection = null;
    private String database;
    private String schema;


    private final DbType dbType = DbType.SNOWFLAKE;
    public static final String ERROR_NO_FIELD_OF_TYPE = "No value was specified for type";
    public static final String ERROR_INVALID_SERVER_STRING = "Server string is not valid";
    public static final String ERROR_INCORRECT_SCHEMA_SPECIFICATION =
            "Database should be specified as 'warehouse.database.schema', " +
                    "e.g. 'computewh.snowflake_sample_data.weather";
    public static final String ERROR_CONNECTION_NOT_INITIALIZED =
            "Snowflake Database connections has not been initialized.";

    SnowflakeConnector() {
    }

    public void resetConnection() throws SQLException {
        if (this.snowflakeConnection != null) {
            this.snowflakeConnection.close();
        }
        this.snowflakeConnection = null;
    }

    public DBConnectorInterface getInstance(IniFile iniFile) {
        getConfiguration(iniFile, null);

        return getInstance(dbSettings.server, dbSettings.database, dbSettings.user, dbSettings.password);
    }

    public DbSettings getConfiguration(IniFile iniFile, PrintStream stream) {
        this.configuration = new SnowFlakeConfiguration();
        ValidationFeedback feedBack = this.configuration.loadAndValidateConfiguration(iniFile);

        if (feedBack.hasErrors()) {
            throw new DBConfigurationException(String.format("There are errors in the configuration:%n\t%s", String.join("\n\t", feedBack.getErrors().keySet())));
        }

        if (feedBack.hasWarnings() && stream != null) {
            stream.printf("The validation of the configuration generated warnings:%n\t%s", String.join("\n\t", feedBack.getWarnings().keySet()));
        }

        String warehouse = configuration.getValue(SNOWFLAKE_WAREHOUSE);
        this.database = configuration.getValue(SNOWFLAKE_DATABASE);
        this.schema = configuration.getValue(SNOWFLAKE_SCHEMA);
        dbSettings = new DbSettings();
        dbSettings.dbType = DbType.SNOWFLAKE;
        dbSettings.server = String.format("https://%s.snowflakecomputing.com", configuration.getValue(SNOWFLAKE_ACCOUNT));
        dbSettings.database = String.format("%s.%s.%s",
                warehouse,
                this.database,
                this.schema);
        dbSettings.domain = dbSettings.database;
        dbSettings.user = configuration.getValue(SNOWFLAKE_USER);
        dbSettings.password = configuration.getValue(SNOWFLAKE_PASSWORD);
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;

        return dbSettings;
    }

    @Override
    public DBConnectorInterface getInstance(String server, String fullSchemaPath, String user, String password) {
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
        return String.format("SELECT COUNT(*) FROM %s.%s.%s;",this.database, this.schema, tableName);
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

    public DbSettings getDbSettings() {
        if (dbSettings == null) {
            throw new RuntimeException(String.format("dbSettings were never initialized for class %s", this.getClass().getName()));
        }

        return dbSettings;
    }

    @Override
    public List<ConfigurationField> getFields() {
        //SnowFlakeConfiguration[] iets = SnowFlakeConfiguration.values();
        return new ArrayList<>();
    }

    private static Connection connectToSnowflake(String server, String schema, String user, String password) {
        try {
            Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Cannot find JDBC driver. Make sure the file snowflake-jdbc-x.xx.xx.jar is in the path: " + ex.getMessage());
        }
        String url = buildUrl(server, schema, user, password, INSTANCE.configuration.getValue(SNOWFLAKE_AUTHENTICATOR));
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

    public DBConfiguration getDBConfiguration() {
        return new SnowFlakeConfiguration();
    }

    public static class SnowFlakeConfiguration extends DBConfiguration {
        public static final String SNOWFLAKE_ACCOUNT = "SNOWFLAKE_ACCOUNT";
        public static final String SNOWFLAKE_USER = "SNOWFLAKE_USER";
        public static final String SNOWFLAKE_PASSWORD = "SNOWFLAKE_PASSWORD";
        public static final String SNOWFLAKE_AUTHENTICATOR = "SNOWFLAKE_AUTHENTICATOR";
        public static final String SNOWFLAKE_WAREHOUSE = "SNOWFLAKE_WAREHOUSE";
        public static final String SNOWFLAKE_DATABASE = "SNOWFLAKE_DATABASE";
        public static final String SNOWFLAKE_SCHEMA = "SNOWFLAKE_SCHEMA";
        public static final String ERROR_MUST_SET_PASSWORD_OR_AUTHENTICATOR = "Either password or authenticator must be specified for Snowflake";
        public static final String ERROR_MUST_NOT_SET_PASSWORD_AND_AUTHENTICATOR = "Specify only one of password or authenticator Snowflake";
        public SnowFlakeConfiguration() {
            super(
                ConfigurationField.create(
                        SNOWFLAKE_ACCOUNT,
                        "Account",
                        "Account for the Snowflake instance")
                        .required(),
                ConfigurationField.create(
                        SNOWFLAKE_USER,
                        "User",
                        "User for the Snowflake instance")
                        .required(),
                ConfigurationField.create(
                        SNOWFLAKE_PASSWORD,
                        "Password",
                        "Password for the Snowflake instance"),
                ConfigurationField.create(
                        SNOWFLAKE_WAREHOUSE,
                        "Warehouse",
                        "Warehouse for the Snowflake instance")
                        .required(),
                ConfigurationField.create(
                        SNOWFLAKE_DATABASE,
                        "Database",
                        "Database for the Snowflake instance")
                        .required(),
                ConfigurationField.create(
                        SNOWFLAKE_SCHEMA,
                        "Schema",
                        "Schema for the Snowflake instance")
                        .required(),
                ConfigurationField.create(
                        SNOWFLAKE_AUTHENTICATOR,
                        "Authenticator method",
                        "Snowflake JDBC authenticator method (only 'externalbrowser' is currently supported)")
            );
            this.configurationFields.addValidator(new PasswordXORAuthenticatorValidator());
        }

        class PasswordXORAuthenticatorValidator implements ConfigurationValidator {

            @Override
            public ValidationFeedback validate(ConfigurationFields fields) {
                ValidationFeedback feedback = new ValidationFeedback();
                String password = fields.getValue(SNOWFLAKE_PASSWORD);
                String authenticator = fields.getValue(SNOWFLAKE_AUTHENTICATOR);
                if (StringUtils.isEmpty(password) && StringUtils.isEmpty(authenticator)) {
                    feedback.addError(ERROR_MUST_SET_PASSWORD_OR_AUTHENTICATOR, fields.get(SNOWFLAKE_PASSWORD));
                    feedback.addError(ERROR_MUST_SET_PASSWORD_OR_AUTHENTICATOR, fields.get(SNOWFLAKE_AUTHENTICATOR));
                } else if (!StringUtils.isEmpty(password) && !StringUtils.isEmpty(authenticator)) {
                    feedback.addError(ERROR_MUST_NOT_SET_PASSWORD_AND_AUTHENTICATOR, fields.get(SNOWFLAKE_PASSWORD));
                    feedback.addError(ERROR_MUST_NOT_SET_PASSWORD_AND_AUTHENTICATOR, fields.get(SNOWFLAKE_AUTHENTICATOR));
                }

                return feedback;
            }
        }
    }

    private static String buildUrl(String server, String schema, String user, String password, String authenticator) {
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
        if (!StringUtils.isEmpty(authenticator)) {
            url = appendParameterIfSet(url, "authenticator", authenticator);
        } else {
            url = appendParameterIfSet(url, "password", password);
        }

        return url;
    }
    private static String appendParameterIfSet(String url, String name, String value) {
        if (!StringUtils.isEmpty(value)) {
            return String.format("%s%s%s=%s", url, (url.endsWith("?") ? "" : "&"), name, value);
        }
        else {
            throw new RuntimeException(String.format(ERROR_NO_FIELD_OF_TYPE + " %s", name));
        }
    }
    private static String[] splitDatabaseName(String databaseName) {
        String[] parts = databaseName.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException(ERROR_INCORRECT_SCHEMA_SPECIFICATION);
        }

        return parts;
    }
}
