package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.ohdsi.databases.SnowflakeConnector.*;
import static org.ohdsi.databases.SnowflakeConnector.ServerConfig.checkSnowflakeConfig;

class SnowflakeConnectorTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "SNOWFLAKE_SERVER", matches = ".*\\.snowflakecomputing\\.com")
    public void testConnectToSnowflake() throws SQLException {
        DbSettings dbSettings = new DbSettings();
        setSnowflakeTestConfig(dbSettings);
        Connection connection = INSTANCE.getInstance(dbSettings).getConnection();

        assertNotNull(connection);
        connection.close();
    }

    @Test
    public void testUninitializedSnowflakeConnection() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            INSTANCE.getInstance().getConnection();
        });

        assertEquals(ERROR_CONNECTION_NOT_INITIALIZED, exception.getMessage());
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

    private static void setSnowflakeTestConfig(DbSettings dbSettings) {
        // get Snowflake settings from the environment
        // this should become the OHDSI Snowflake test instance once that is available
        dbSettings.server = checkAndGetEnv("SNOWFLAKE_SERVER");
        dbSettings.user = checkAndGetEnv("SNOWFLAKE_USER");
        dbSettings.password = checkAndGetEnv("SNOWFLAKE_PASSWORD");
        dbSettings.database = checkAndGetEnv("SNOWFLAKE_DATABASE");
        dbSettings.domain = dbSettings.database;
    }

    private static String checkAndGetEnv(String name) {
        String value = System.getenv(name);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException(String.format("Environment variable '%s' is not set.", name));
        }

        return value;
    }
}