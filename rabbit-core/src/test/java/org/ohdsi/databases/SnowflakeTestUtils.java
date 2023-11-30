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
