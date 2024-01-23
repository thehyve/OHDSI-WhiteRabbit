package org.ohdsi.databases;

import org.ohdsi.databases.configuration.*;
import org.ohdsi.utilities.collections.Pair;
import org.ohdsi.utilities.files.IniFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.ohdsi.databases.MySqlHandler.MySqlConfiguration.*;

public enum MySqlHandler implements StorageHandler {
    INSTANCE();

    private DBConnection mySqlConnection = null;

    final static Logger logger = LoggerFactory.getLogger(MySqlHandler.class);

    MySqlConfiguration configuration = new MySqlHandler.MySqlConfiguration();

    private static final DbType dbType = DbType.MYSQL_NEW;

    @Override
    public StorageHandler getInstance(DbSettings dbSettings) {
        if (mySqlConnection == null) {
            mySqlConnection = getDBConnection();
        }
        return INSTANCE;
    }

    @Override
    public DBConnection getDBConnection() {
        if (mySqlConnection == null) {
            mySqlConnection = getDBConnection(configuration.toDbSettings(new ValidationFeedback()));
        }

        return mySqlConnection;
    }

    private DBConnection getDBConnection(DbSettings dbSettings) {
        return connectToMySql(dbSettings);
    }

    @Override
    public void close() throws SQLException {
        if (mySqlConnection != null) {
            mySqlConnection.close();
            mySqlConnection = null;
        }
    }

    @Override
    public DbType getDbType() {
        return dbType;
    }

    @Override
    public void checkInitialised() throws DBConfigurationException {

    }

    @Override
    public void use(String database) {
        getDBConnection().use(database, dbType);
    }

    @Override
    public String getDatabase() {
        return configuration.getValue(MYSQL_DATABASE);
    }

    @Override
    public String getTablesQuery(String database) {
        return String.format("SHOW TABLES IN %s", database);
    }

    @Override
    public String getRowSampleQuery(String table, long rowCount, long sampleSize) {
        return String.format("SELECT * FROM %s ORDER BY RAND() LIMIT %s", table, sampleSize);
    }

    @Override
    public DBConfiguration getDBConfiguration() {
        return null;
    }

    private DBConnection connectToMySql(DbSettings dbSettings) {
        String url = "jdbc:mysql://" + dbSettings.server + "?useCursorFetch=true&zeroDateTimeBehavior=convertToNull";

        try {
            Connection jdbcConnection = DriverManager.getConnection(url, dbSettings.user, dbSettings.password);
            DBConnection dbConnection = new DBConnection(jdbcConnection, DbType.MYSQL_NEW, false);
            return dbConnection;
        } catch (SQLException e1) {
            throw new RuntimeException("Cannot connect to DB server: " + e1.getMessage());
        }
    }

    public static Pair<MySqlHandler.MySqlConfiguration, DbSettings> getConfiguration(IniFile iniFile, ValidationFeedback feedback) {
        MySqlHandler.MySqlConfiguration configuration = new MySqlHandler.MySqlConfiguration();
        ValidationFeedback currentFeedback = configuration.loadAndValidateConfiguration(iniFile);
        if (feedback != null) {
            feedback.add(currentFeedback);
        }

        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = dbType;
        dbSettings.server = configuration.getValue(MYSQL_SERVER) + ":" + configuration.getValue(MYSQL_PORT);
        dbSettings.database = configuration.getValue(MYSQL_DATABASE);
        dbSettings.domain = dbSettings.database;
        dbSettings.user = configuration.getValue(MYSQL_USER);
        dbSettings.password = configuration.getValue(MYSQL_PASSWORD);
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;

        return new Pair<>(configuration, dbSettings);
    }

    public void setConfiguration(String server, String port, String database, String user, String password) {
        configuration.setConfiguration(server, port, database, user, password);
    }

    public static class MySqlConfiguration extends DBConfiguration {
        public static final String MYSQL_SERVER = "MYSQL_SERVER";
        public static final String TOOLTIP_MYSQL_SERVER = "Name or IP address of the database server";
        public static final String MYSQL_PORT = "MYSQL_PORT";
        public static final String TOOLTIP_MYSQL_PORT = "The IP port of the database server (usually 3306)";
        public static final String MYSQL_DATABASE = "MYSQL_DATABASE";
        public static final String TOOLTIP_MYSQL_DATABASE = "The name of the database containing the source tables";
        public static final String MYSQL_USER = "MYSQL_USER";
        public static final String TOOLTIP_MYSQL_USER = "The user used to log in to the server";
        public static final String MYSQL_PASSWORD = "MYSQL_PASSWORD";
        public static final String TOOLTIP_MYSQL_PASSWORD = "The password used to log in to the server";

        public MySqlConfiguration() {
            super(
                    ConfigurationField.create(
                                    MYSQL_SERVER,
                                    "Server",
                                    TOOLTIP_MYSQL_SERVER)
                            .required(),
                    ConfigurationField.create(
                                    MYSQL_PORT,
                                    "Port",
                                    TOOLTIP_MYSQL_PORT)
                            .defaultValue("3306")
                            .required(),
                    ConfigurationField.create(
                                    MYSQL_DATABASE,
                                    "Database",
                                    TOOLTIP_MYSQL_DATABASE)
                            .required(),
                    ConfigurationField.create(
                                    MYSQL_USER,
                                    "User",
                                    TOOLTIP_MYSQL_USER)
                            .required(),
                    ConfigurationField.create(
                            MYSQL_PASSWORD,
                            "Password",
                            TOOLTIP_MYSQL_PASSWORD)
            );
        }

        @Override
        public DbSettings toDbSettings(ValidationFeedback feedback) {
            return getConfiguration(this.toIniFile(),feedback ).getItem2();
        }

        public void setConfiguration(String server, String port, String database, String user, String password) {
            IniFile inifile = new IniFile();
            inifile.set(MYSQL_SERVER, server);
            inifile.set(MYSQL_PORT, port);
            inifile.set(MYSQL_DATABASE, database);
            inifile.set(MYSQL_USER, user);
            inifile.set(MYSQL_PASSWORD, password);
            this.loadAndValidateConfiguration(inifile);
        }
    }
}
