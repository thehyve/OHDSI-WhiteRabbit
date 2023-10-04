package org.ohdsi.databases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.ohdsi.databases.SnowflakeConnector.*;
import static org.ohdsi.databases.SnowflakeConnector.ServerConfig.checkSnowflakeConfig;

class TestSnowflakeConnector {
    @Test
    @EnabledIfEnvironmentVariable(named = "SNOWFLAKE_TEST_SERVER", matches = ".*\\.snowflakecomputing\\.com")
    public void testConnectToSnowflake() throws SQLException {
        DbSettings dbSettings = new DbSettings();
        SnowflakeTestUtils.setSnowflakeTestConfig(dbSettings);
        Connection connection = INSTANCE.getInstance(dbSettings).getConnection();

        assertNotNull(connection);
        connection.close();
    }

    @Test
    public void testUninitializedSnowflakeConnection() throws SQLException {
        INSTANCE.resetConnection(); // make sure the connection is not available from running another test first
        Exception exception = assertThrows(RuntimeException.class, () -> {
            INSTANCE.getInstance().getConnection();
        });

        assertEquals(ERROR_CONNECTION_NOT_INITIALIZED, exception.getMessage());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SNOWFLAKE_TEST_SERVER", matches = ".*\\.snowflakecomputing\\.com")
    public void testGetFieldNames() {
        DbSettings dbSettings = new DbSettings();
        SnowflakeTestUtils.setSnowflakeTestConfig(dbSettings);
        Connection connection = INSTANCE.getInstance(dbSettings).getConnection();
        ResultSet resultSet = INSTANCE.getFieldNames("customer");
        try {
            while (resultSet.next()) {
                assertEquals(resultSet.getString("COLUMN_NAME"), "iets");
                assertEquals(resultSet.getString("TYPE_NAME"), "iets");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    void testCheckSnowflakeConfig() {
        List<String> errors;

        // correct case, all required values in the server string
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "someuser", "somepassword", "wh.db.schema");
        assertEquals(0, errors.size());

        // incorrect case, schema not complete
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "someuser", "somepassword", "first.second");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_INCORRECT_SCHEMA_SPECIFICATION));

        // incorrect case, schema not complete
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "someuser", "somepassword", "first");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_INCORRECT_SCHEMA_SPECIFICATION));

        // incorrect case, schema not correct (has 4 parts)
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "someuser", "somepassword", "first.second.third.fourth");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_INCORRECT_SCHEMA_SPECIFICATION));

        // no password specified at all should result in a warning
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "someuser", "", "first.second.third");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " password"));

        // incorrect case, user must be specified
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "", "somepassword", "first.second.third");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " user"));

        // incorrect case, schema must be specified
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "someuser", "somepassword", "");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " database"));

        // incorrect case, nothing specified, results in multiple errors
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
                "", "", "");
        assertEquals(3, errors.size());
        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " database"));
        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " user"));
        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " password"));

        // incorrect case: invalid server string (space before '/')
        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com /",
                "someuser", "somepassword", "wh.db.sch");
        assertEquals(1, errors.size());
        assertTrue(errorsContain(errors, ERROR_INVALID_SERVER_STRING));
    }

    private static boolean errorsContain(List<String> errors, String messageStart) {
        for (String error: errors) {
            if (error.startsWith(messageStart)) {
                return true;
            }
        }

        return false;
    }
}