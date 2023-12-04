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
import org.ohdsi.databases.configuration.DbSettings;
import org.ohdsi.databases.configuration.DbType;

import java.util.Arrays;

public class SnowflakeTestUtils {

    public static DbSettings getTestDbSettingsSnowflake() {
        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = DbType.SNOWFLAKE;
        dbSettings.tables = Arrays.asList("PERSON", "COST");
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;
        dbSettings.user = SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_USER");
        dbSettings.password = SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_PASSWORD");
        dbSettings.server = String.format("https://%s.snowflakecomputing.com", SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_ACCOUNT"));
        dbSettings.database = String.format("%s.%s.%s",
                SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_WAREHOUSE"),
                SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_DATABASE"),
                SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_SCHEMA"));
        dbSettings.domain = dbSettings.database;

        return dbSettings;
    }


    public static String getenvOrFail(String name) {
        String value = System.getenv(name);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException(String.format("Environment variable '%s' is not set.", name));
        }

        return value;
    }
}
