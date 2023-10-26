package org.ohdsi.whiterabbit.scan;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbSettings;
import org.ohdsi.databases.SnowflakeTestUtils;
import org.ohdsi.whiteRabbit.scan.SourceDataScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SourceDataScanSnowflakeIT {

    public final static String SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE = "SNOWFLAKE_WR_TEST_ACCOUNT";
    Logger logger = LoggerFactory.getLogger(SourceDataScanSnowflakeIT.class);

    final String CONTAINER_DATA_PATH = "/scan_data";
    @Container
    public static GenericContainer<?> testContainer = createPythonContainer();

    @TempDir
    static File tmpDir;

    @Test
    void testWarnWhenRunningWithoutSnowflakeConfigured() {
        String snowflakeWrTestAoount = System.getenv(SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE);
        assertFalse(StringUtils.isEmpty(snowflakeWrTestAoount),
                String.format("\nTest class %s is being run without a Snowflake test instance configured.\n" +
                        "This is NOT a valid verification run.", SourceDataScanSnowflakeIT.class.getName()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE, matches = ".+")
    void testProcessSnowflake(@TempDir Path tempDir) throws IOException, InterruptedException {
        prepareTestData();
        Path outFile = tempDir.resolve("scanresult-snowflake.xslx");
        SourceDataScan sourceDataScan = new SourceDataScan();
        DbSettings dbSettings = SnowflakeTestUtils.getTestDbSettingsSnowflake();

        sourceDataScan.process(dbSettings, outFile.toString());
        ScanTestUtils.verifyScanResultsFromXSLX(outFile, dbSettings.dbType);

        logger.info("Testing scan on Snowflake OK");
    }

    private static void prepareTestData() throws IOException, InterruptedException {
        // snowsql is used for initializing the database

        // add some packages neede for the installation of snowsql
        execAndVerifyCommand(testContainer, "/bin/sh", "-c", "apt update; apt -y install wget unzip");
        // download snowsql
        execAndVerifyCommand(testContainer, "/bin/bash", "-c",
                "wget -q https://sfc-repo.snowflakecomputing.com/snowsql/bootstrap/1.2/linux_x86_64/snowsql-1.2.29-linux_x86_64.bash;");
        // install snowsql
        execAndVerifyCommand(testContainer, "/bin/bash", "-c",
                "echo -e \"/tmp\\nN\" | bash snowsql-1.2.29-linux_x86_64.bash ");

        // run the sql script needed to initialize the test data
        execAndVerifyCommand(testContainer, "/bin/bash", "-c",
                String.format("(cd /scan_data; SNOWSQL_PWD='%s' /tmp/snowsql -a %s -u %s -d %s -s %s -f /scan_data/create_data_snowflake.sql)",
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_PASSWORD"),
                        SnowflakeTestUtils.getenvOrFail(SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE),
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_USER"),
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_DATABASE"),
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_SCHEMA")
                ));
    }

    public static GenericContainer<?> createPythonContainer() {
        GenericContainer<?> testContainer = new GenericContainer<>(DockerImageName.parse("ubuntu:22.04"))
                .withCommand("/bin/sh", "-c", "tail -f /dev/null") // keeps the container running until it is explicitly stopped
                .withClasspathResourceMapping(
                        "scan_data",
                        "/scan_data",
                        BindMode.READ_ONLY);

        testContainer.start();

        return testContainer;
    }

    private static void execAndVerifyCommand(GenericContainer<?> container, String... command) throws IOException, InterruptedException {
        execAndVerifyCommand(container, 0, command);
    }
    private static void execAndVerifyCommand(GenericContainer<?> container, int expectedExitValue, String... command) throws IOException, InterruptedException {
        org.testcontainers.containers.Container.ExecResult result;

        result = container.execInContainer(command);
        if (result.getExitCode() != expectedExitValue) {
            System.out.println("stdout: " + result.getStdout());
            System.out.println("stderr: " + result.getStderr());
            // hide the password, if present, so it won't appear in logs (pragmatic)
            String message = ("Command failed: " + String.join(" ", command))
                    .replace(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_PASSWORD"), "xxxxx");
            assertEquals(expectedExitValue, result.getExitCode(), message);
        }
    }
}
