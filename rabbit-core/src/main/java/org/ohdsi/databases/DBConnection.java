package org.ohdsi.databases;

import java.sql.*;

/*
 * DBConnection is a wrapper for java.sql.Connection and org.ohdsi.databases.DBConnectionInterface
 * The latter one instantiates a java.sql.Connection instance itself.
 * The constructors of DBConnection ensure that one of the following is true:
 *  - a java.sql.Connection implementing object is provided, and used it its methods
 *  - a DBConnectionInterface implementing object is provided, and used to create a java.sql.Connection interface
 *  - if neither of the above is valid at construction, a RuntimeException is thrown
 *
 * DBConnection provides a partial subset of the java.sql.Connection interface, just enough to satisfy the]
 * needs of WhiteRabbit
 */
public class DBConnection {
    private final Connection connection;
    private final DBConnectorInterface connector;

    public DBConnection(Connection connection) {
        this.connection = connection;
        this.connector = null;
    }

    public DBConnection(DBConnectorInterface connector) {
        this.connector = connector;
        connector.checkInitialised();
        this.connection = connector.getConnection();
    }

    public Connection getConnection() {
        return this.connection;
    }

    public DBConnectorInterface getConnector() {
        this.connector.checkInitialised();
        return this.connector;
    }

    public boolean hasDBConnector() {
        return this.connector != null;
    }

    public Statement createStatement(int typeForwardOnly, int concurReadOnly) throws SQLException {
        return this.connection.createStatement(typeForwardOnly, concurReadOnly);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return this.connection.getMetaData();
    }

    public void close() throws SQLException {
        this.connection.close();
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
