package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.ohdsi.databases.configuration.ConfigurationField;
import org.ohdsi.databases.configuration.DBConfiguration;
import org.ohdsi.databases.configuration.DBConfigurationException;
import org.ohdsi.utilities.files.IniFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.ohdsi.databases.SnowflakeConnector.*;

class TestSnowflakeConnector {

    Logger logger = LoggerFactory.getLogger(TestSnowflakeConnector.class);
//    @Test
//    @EnabledIfEnvironmentVariable(named = "SNOWFLAKE_WR_TEST_ACCOUNT", matches = ".+")
//    public void testConnectToSnowflake() throws SQLException {
//        DbSettings dbSettings = SnowflakeTestUtils.getTestDbSettingsSnowflake();
//        Connection connection = INSTANCE.getInstance(dbSettings).getConnection();
//
//        assertNotNull(connection);
//        connection.close();
//        logger.info("Connecting to Snowflake OK");
//    }

    @Test
    public void testUninitializedSnowflakeConnection() throws SQLException {
        INSTANCE.resetConnection(); // make sure the connection is not available from running another test first
        Exception exception = assertThrows(RuntimeException.class, () -> {
            INSTANCE.getInstance().getDBConnection();
        });

        assertEquals(ERROR_CONNECTION_NOT_INITIALIZED, exception.getMessage());
    }

//    @Test
//    @EnabledIfEnvironmentVariable(named = "SNOWFLAKE_WR_TEST_ACCOUNT", matches = ".+")
//    void testGetFieldNames() {
//        DbSettings dbSettings = SnowflakeTestUtils.getTestDbSettingsSnowflake();
//        Connection connection = INSTANCE.getInstance(dbSettings).getConnection();
//        ResultSet resultSet = INSTANCE.getFieldNames("person");
//        try {
//            while (resultSet.next()) {
//                assertEquals(resultSet.getString("COLUMN_NAME"), "iets");
//                assertEquals(resultSet.getString("TYPE_NAME"), "iets");
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e.getMessage());
//        }
//        logger.info("Testing field names for Snowflake OK");
//    }

    @Test
    void testIniFileAuthenticatorMethod() {
        IniFile iniFile = new IniFile(TestSnowflakeConnector.class.getClassLoader().getResource("snowflake.ini").getFile());

        assertNotNull(SnowflakeConnector.INSTANCE.getConfiguration(iniFile, null));

        iniFile.set("SNOWFLAKE_PASSWORD", "");
        Exception exception = assertThrows(DBConfigurationException.class, () -> {
            assertNotNull(SnowflakeConnector.INSTANCE.getConfiguration(iniFile, null));
        });
        assertTrue(exception.getMessage().contains(SnowflakeConfiguration.ERROR_MUST_SET_PASSWORD_OR_AUTHENTICATOR));

        iniFile.set("SNOWFLAKE_AUTHENTICATOR", "externalbrowser");
        assertNotNull(SnowflakeConnector.INSTANCE.getConfiguration(iniFile, null));

        iniFile.set("SNOWFLAKE_PASSWORD", "some-password");
        exception = assertThrows(DBConfigurationException.class, () -> {
            assertNotNull(SnowflakeConnector.INSTANCE.getConfiguration(iniFile, null));
        });
        assertTrue(exception.getMessage().contains(SnowflakeConfiguration.ERROR_MUST_NOT_SET_PASSWORD_AND_AUTHENTICATOR));
    }

    @Test
    void testPrintIniFileTemplate() throws IOException {
        String output;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); PrintStream printStream = new PrintStream(outputStream)) {
            DBConfiguration configuration = new SnowflakeConfiguration();
            configuration.printIniFileTemplate(printStream);
            output = outputStream.toString();
            for (ConfigurationField field: configuration.getFields()) {
                assertTrue(output.contains(field.name), String.format("ini file template should contain field name (%s)", field.name));
                assertTrue(output.contains(field.toolTip), String.format("ini file template should contain tool tip (%s)", field.toolTip));
                if (!StringUtils.isEmpty(field.getDefaultValue())) {
                    assertTrue(output.contains(field.getDefaultValue()), String.format("ini file template should contain default value (%s)", field.getDefaultValue()));
                }
            }
        }
    }
//    @Test
//    void testCheckSnowflakeConfig() {
//        List<String> errors;
//
//        // correct case, all required values in the server string
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "someuser", "somepassword", "wh.db.schema");
//        assertEquals(0, errors.size());
//
//        // incorrect case, schema not complete
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "someuser", "somepassword", "first.second");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_INCORRECT_SCHEMA_SPECIFICATION));
//
//        // incorrect case, schema not complete
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "someuser", "somepassword", "first");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_INCORRECT_SCHEMA_SPECIFICATION));
//
//        // incorrect case, schema not correct (has 4 parts)
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "someuser", "somepassword", "first.second.third.fourth");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_INCORRECT_SCHEMA_SPECIFICATION));
//
//        // no password specified at all should result in a warning
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "someuser", "", "first.second.third");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " password"));
//
//        // incorrect case, user must be specified
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "", "somepassword", "first.second.third");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " user"));
//
//        // incorrect case, schema must be specified
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "someuser", "somepassword", "");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " database"));
//
//        // incorrect case, nothing specified, results in multiple errors
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com/",
//                "", "", "");
//        assertEquals(3, errors.size());
//        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " database"));
//        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " user"));
//        assertTrue(errorsContain(errors, ERROR_NO_FIELD_OF_TYPE + " password"));
//
//        // incorrect case: invalid server string (space before '/')
//        errors = checkSnowflakeConfig("someaccount-id.snowflakecomputing.com /",
//                "someuser", "somepassword", "wh.db.sch");
//        assertEquals(1, errors.size());
//        assertTrue(errorsContain(errors, ERROR_INVALID_SERVER_STRING));
//    }

//    private static boolean errorsContain(List<String> errors, String messageStart) {
//        for (String error: errors) {
//            if (error.startsWith(messageStart)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
}