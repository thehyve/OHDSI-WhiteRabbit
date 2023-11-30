package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;
import org.ohdsi.utilities.files.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/*
 * DBConnection is a wrapper for java.sql.Connection and org.ohdsi.databases.DBConnectionInterface
 * The latter one instantiates a java.sql.Connection instance itself.
 * The constructors of DBConnection ensure that one of the following is true:
 *  - a java.sql.Connection implementing object is provided, and used it its methods
 *  - a DBConnectionInterface implementing object is provided, and used to create a java.sql.Connection interface
 *  - if neither of the above is valid at construction, a RuntimeException is thrown
 *
 * DBConnection provides a partial subset of the java.sql.Connection interface, just enough to satisfy the
 * needs of WhiteRabbit
 */
public class DBConnection {
    Logger logger = LoggerFactory.getLogger(DBConnection.class);

    private final Connection connection;
    private final DbType dbType;
    private boolean verbose;
    private final DBConnectorInterface connectorInterface;
    private static DecimalFormat decimalFormat		= new DecimalFormat("#.#");


    public DBConnection(Connection connection, DbType dbType, boolean verbose) {
        this.connection = connection;
        this.dbType = dbType;
        this.connectorInterface = null;
        this.verbose = verbose;
    }

    public DBConnection(DBConnectorInterface connectorInterface, DbType dbType, boolean verbose) {
        this.connectorInterface = connectorInterface;
        connectorInterface.checkInitialised();
        this.connection = connectorInterface.getDBConnection().getConnection();
        this.dbType = dbType;
        this.verbose = verbose;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public DBConnectorInterface getDBConnectorInterface() {
        this.connectorInterface.checkInitialised();
        return this.connectorInterface;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean hasDBConnectorInterface() {
        return this.connectorInterface != null;
    }

    public Statement createStatement(int typeForwardOnly, int concurReadOnly) throws SQLException {
        return this.connection.createStatement(typeForwardOnly, concurReadOnly);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return this.connection.getMetaData();
    }

    public void use(String database, DbType dbType) {
        if (this.hasDBConnectorInterface()) {
            this.getDBConnectorInterface().use(database);
        } else {
            if (database == null || dbType == DbType.MS_ACCESS || dbType == DbType.BIGQUERY || dbType == DbType.AZURE) {
                return;
            }

            if (dbType == DbType.ORACLE) {
                execute("ALTER SESSION SET current_schema = " + database);
            } else if (dbType == DbType.POSTGRESQL || dbType == DbType.REDSHIFT) {
                execute("SET search_path TO " + database);
            } else if (dbType == DbType.TERADATA) {
                execute("database " + database);
            } else {
                execute("USE " + database);
            }
        }
    }

    public void execute(String sql) {
        execute(sql, false);
    }

    public void execute(String sql, boolean verbose) {
        Statement statement = null;
        try {
            if (StringUtils.isEmpty(sql)) {
                return;
            }

            statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            for (String subQuery : sql.split(";")) {
                if (verbose) {
                    String abbrSQL = subQuery.replace('\n', ' ').replace('\t', ' ').trim();
                    if (abbrSQL.length() > 100)
                        abbrSQL = abbrSQL.substring(0, 100).trim() + "...";
                    logger.info("Adding query to batch: " + abbrSQL);
                }

                statement.addBatch(subQuery);
            }
            long start = System.currentTimeMillis();
            if (verbose) {
                logger.info("Executing batch");
            }
            statement.executeBatch();
            if (verbose) {
                outputQueryStats(statement, System.currentTimeMillis() - start);
            }
        } catch (SQLException e) {
            logger.error(sql);
            logger.error(e.getMessage(), e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    void outputQueryStats(Statement statement, long ms) throws SQLException {
        Throwable warning = statement.getWarnings();
        if (warning != null)
            logger.info("- SERVER: " + warning.getMessage());
        String timeString;
        if (ms < 1000)
            timeString = ms + " ms";
        else if (ms < 60000)
            timeString = decimalFormat.format(ms / 1000d) + " seconds";
        else if (ms < 3600000)
            timeString = decimalFormat.format(ms / 60000d) + " minutes";
        else
            timeString = decimalFormat.format(ms / 3600000d) + " hours";
        logger.info("- Query completed in " + timeString);
    }

    public List<String> getTableNames(String database) {
        if (this.hasDBConnectorInterface()) {
            return this.getDBConnectorInterface().getTableNames();
        } else {
            return getTableNamesClassic(database);
        }
    }

    private List<String> getTableNamesClassic(String database) {
        List<String> names = new ArrayList<>();
        String query = null;
        if (dbType == DbType.MYSQL) {
            query = "SHOW TABLES IN " + database;
        } else if (dbType == DbType.SQL_SERVER || dbType == DbType.PDW || dbType == DbType.AZURE) {
            query = "SELECT CONCAT(schemas.name, '.', tables_views.name) FROM " +
                    "(SELECT schema_id, name FROM %1$s.sys.tables UNION ALL SELECT schema_id, name FROM %1$s.sys.views) tables_views " +
                    "INNER JOIN %1$s.sys.schemas ON tables_views.schema_id = schemas.schema_id " +
                    "ORDER BY schemas.name, tables_views.name";
            query = String.format(query, database);
            logger.info(query);
        } else if (dbType == DbType.ORACLE) {
            query = "SELECT table_name FROM " +
                    "(SELECT table_name, owner FROM all_tables UNION ALL SELECT view_name, owner FROM all_views) tables_views " +
                    "WHERE owner='" + database.toUpperCase() + "'";
        } else if (dbType == DbType.POSTGRESQL || dbType == DbType.REDSHIFT) {
            query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + database.toLowerCase() + "' ORDER BY table_name";
        } else if (dbType == DbType.MS_ACCESS) {
            query = "SELECT Name FROM sys.MSysObjects WHERE (Type=1 OR Type=5) AND Flags=0;";
        } else if (dbType == DbType.TERADATA) {
            query = "SELECT TableName from dbc.tables WHERE tablekind IN ('T','V') and databasename='" + database + "'";
        } else if (dbType == DbType.BIGQUERY) {
            query = "SELECT table_name from " + database + ".INFORMATION_SCHEMA.TABLES ORDER BY table_name;";
        } else if (dbType == DbType.SNOWFLAKE) {

        }

        for (Row row : query(query))
            names.add(row.get(row.getFieldNames().get(0)));
        return names;
    }

    public QueryResult query(String sql) {
        return new QueryResult(sql, this, verbose);
    }

    public void close() throws SQLException {
        if (this.hasDBConnectorInterface()) {
            this.getDBConnectorInterface().close();
        } else {
            this.connection.close();
        }
    }

    public void setAutoCommit(boolean b) throws SQLException {
        this.connection.setAutoCommit(b);
    }

    public PreparedStatement prepareStatement(String statement) throws SQLException {
        return this.connection.prepareStatement(statement);
    }

    public void commit() throws SQLException {
        this.connection.commit();
    }

    public void clearWarnings() throws SQLException {
        this.connection.clearWarnings();
    }
}
