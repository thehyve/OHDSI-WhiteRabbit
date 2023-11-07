package org.ohdsi.whiterabbit.scan;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbSettings;
import org.ohdsi.databases.DbType;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SourceDataScanSnowflakeIT {

    public final static String SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE = "SNOWFLAKE_WR_TEST_ACCOUNT";
    Logger logger = LoggerFactory.getLogger(SourceDataScanSnowflakeIT.class);

    final static String CONTAINER_DATA_PATH = "/scan_data";
    @Container
    public static GenericContainer<?> testContainer = createPythonContainer();

    @TempDir
    static File tmpDir;

    @Test
    void testWarnWhenRunningWithoutSnowflakeConfigured() {
        String snowflakeWrTestAccunt = System.getenv(SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE);
        assertFalse(StringUtils.isEmpty(snowflakeWrTestAccunt) && StringUtils.isEmpty(System.getProperty("ohdsi.org.whiterabbit.skip_snowflake_tests")),
                String.format("\nTest class %s is being run without a Snowflake test instance configured.\n" +
                        "This is NOT a valid verification run.", SourceDataScanSnowflakeIT.class.getName()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE, matches = ".+")
    void testProcessSnowflake(@TempDir Path tempDir) throws IOException, InterruptedException, URISyntaxException {
        prepareTestData();
        Path outFile = tempDir.resolve("scanresult-snowflake.xlsx");
        URL referenceScanReport = TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-sql.xlsx");

        SourceDataScan sourceDataScan = ScanTestUtils.createSourceDataScan();
        DbSettings dbSettings = SnowflakeTestUtils.getTestDbSettingsSnowflake();

        sourceDataScan.process(dbSettings, outFile.toString());
        ScanTestUtils.compareScanResultsToReference(outFile, Paths.get(referenceScanReport.toURI()), DbType.SNOWFLAKE);

        logger.info("Testing scan on Snowflake OK");
    }

    private static void prepareTestData() throws IOException, InterruptedException {
        // snowsql is used for initializing the database

        // add some packages needed for the installation of snowsql
        execAndVerifyCommand(testContainer, "/bin/sh", "-c", "apt update; apt -y install wget unzip");
        // download snowsql
        execAndVerifyCommand(testContainer, "/bin/bash", "-c",
                "wget -q https://sfc-repo.snowflakecomputing.com/snowsql/bootstrap/1.2/linux_x86_64/snowsql-1.2.29-linux_x86_64.bash;");
        // install snowsql
        execAndVerifyCommand(testContainer, "/bin/bash", "-c",
                "echo -e \"/tmp\\nN\" | bash snowsql-1.2.29-linux_x86_64.bash ");

        // run the sql script needed to initialize the test data
        execAndVerifyCommand(testContainer, "/bin/bash", "-c",
                String.format("(cd %s; SNOWSQL_PWD='%s' /tmp/snowsql -a %s -u %s -d %s -s %s -f %s/create_data_snowflake.sql)",
                        CONTAINER_DATA_PATH,
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_PASSWORD"),
                        SnowflakeTestUtils.getenvOrFail(SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE),
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_USER"),
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_DATABASE"),
                        SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_SCHEMA"),
                        CONTAINER_DATA_PATH
                        ));
    }

    public static GenericContainer<?> createPythonContainer() {
        GenericContainer<?> testContainer = new GenericContainer<>(DockerImageName.parse("ubuntu:22.04"))
                .withCommand("/bin/sh", "-c", "tail -f /dev/null") // keeps the container running until it is explicitly stopped
                .withClasspathResourceMapping(
                        "scan_data",
                        CONTAINER_DATA_PATH,
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
