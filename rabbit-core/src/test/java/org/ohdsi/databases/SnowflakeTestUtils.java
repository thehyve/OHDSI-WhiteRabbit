package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;

public class SnowflakeTestUtils {

    public static void setSnowflakeTestConfig(DbSettings dbSettings) {
        // get Snowflake settings from the environment
        // this should become the OHDSI Snowflake test instance once that is available
        dbSettings.server = getenvOrFail("SNOWFLAKE_TEST_SERVER");
        dbSettings.user = getenvOrFail("SNOWFLAKE_TEST_USER");
        dbSettings.password = getenvOrFail("SNOWFLAKE_TEST_PASSWORD");
        dbSettings.database = getenvOrFail("SNOWFLAKE_TEST_DATABASE");
        dbSettings.domain = dbSettings.database;
    }

    private static String getenvOrFail(String name) {
        String value = System.getenv(name);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException(String.format("Environment variable '%s' is not set.", name));
        }

        return value;
    }

}
