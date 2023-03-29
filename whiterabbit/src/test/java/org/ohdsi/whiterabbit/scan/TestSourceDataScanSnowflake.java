package org.ohdsi.whiterabbit.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbSettings;
import org.ohdsi.databases.DbType;
import org.ohdsi.databases.SnowflakeTestUtils;
import org.ohdsi.whiteRabbit.scan.SourceDataScan;

import java.nio.file.Path;
import java.util.Arrays;

public class TestSourceDataScanSnowflake {
    @Test
    @EnabledIfEnvironmentVariable(named = "SNOWFLAKE_SERVER", matches = ".*\\.snowflakecomputing\\.com")
    void testProcessSnowflake(@TempDir Path tempDir) {
        Path outFile = tempDir.resolve("scanresult.xslx");
        //Path outFile = tempDir.resolve("/tmp/scanresult.xslx");
        SourceDataScan sourceDataScan = new SourceDataScan();
        DbSettings dbSettings = getTestDbSettingsSnowflake();

        sourceDataScan.process(dbSettings, outFile.toString());
    }

    private DbSettings getTestDbSettingsSnowflake() {
        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = DbType.SNOWFLAKE;
        dbSettings.database = "TPCH_SF1";
        dbSettings.tables = Arrays.asList("CUSTOMER"); //, "DAILY_16_TOTAL", "HOURLY_14_TOTAL", "HOURLY_16_TOTAL",
        // "WEATHER_14_TOTAL");
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;
        SnowflakeTestUtils.setSnowflakeTestConfig(dbSettings);

        return dbSettings;
    }

    private static void setSnowflakeTestConfig(DbSettings dbSettings) {
        // these are currently from a developer account created by Jan Blom
        // this should become the OHDSI Snowflake test instance once that is available
        dbSettings.server = "bomhulx-vw21434.snowflakecomputing.com";
        dbSettings.user = "pidrebznl";
        dbSettings.password = "LetMeIn-123";
        dbSettings.database = "compute_wh.snowflake_sample_data.tpch_sf1";
        dbSettings.domain = dbSettings.database;
    }
}
