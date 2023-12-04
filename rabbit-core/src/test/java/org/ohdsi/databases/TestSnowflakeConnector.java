/*******************************************************************************
 * Copyright 2023 Observational Health Data Sciences and Informatics & The Hyve
 *
 * This file is part of WhiteRabbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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

import static org.junit.jupiter.api.Assertions.*;
import static org.ohdsi.databases.SnowflakeConnection.*;

class TestSnowflakeConnector {

    Logger logger = LoggerFactory.getLogger(TestSnowflakeConnector.class);

    @Test
    void testIniFileAuthenticatorMethod() {
        IniFile iniFile = new IniFile(TestSnowflakeConnector.class.getClassLoader().getResource("snowflake.ini").getFile());

        assertNotNull(SnowflakeConnection.INSTANCE.getConfiguration(iniFile, null));

        iniFile.set("SNOWFLAKE_PASSWORD", "");
        Exception exception = assertThrows(DBConfigurationException.class, () -> {
            assertNotNull(SnowflakeConnection.INSTANCE.getConfiguration(iniFile, null));
        });
        assertTrue(exception.getMessage().contains(SnowflakeConfiguration.ERROR_MUST_SET_PASSWORD_OR_AUTHENTICATOR));

        iniFile.set("SNOWFLAKE_AUTHENTICATOR", "externalbrowser");
        assertNotNull(SnowflakeConnection.INSTANCE.getConfiguration(iniFile, null));

        iniFile.set("SNOWFLAKE_PASSWORD", "some-password");
        exception = assertThrows(DBConfigurationException.class, () -> {
            assertNotNull(SnowflakeConnection.INSTANCE.getConfiguration(iniFile, null));
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