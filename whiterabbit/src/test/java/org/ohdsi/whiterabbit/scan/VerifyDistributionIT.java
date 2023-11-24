package org.ohdsi.whiterabbit.scan;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbType;
import org.ohdsi.utilities.files.IniFile;
import org.ohdsi.whiteRabbit.WhiteRabbitMain;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Intent: "deploy" the distributed application in a docker container containing a Java runtime of a specified version,
 * and run a set of tests that aim to verify that the distribution is complete, i.e. no dependencies
 * are missing. A data for a scan on csv files is used to run whiterabbit.
 *
 * Note that this does not test any of the JDBC driver dependencies, unless these databases are actually used.
 */
public class VerifyDistributionIT {

    @TempDir
    static Path tempDir;

    private static final String WORKDIR_IN_CONTAINER = "/whiterabbit";
    private static final String APPDIR_IN_CONTAINER = "/app";

    @Test
    void testDistributionWithJava8() throws IOException, URISyntaxException, InterruptedException {
        testWhiteRabbitInContainer("eclipse-temurin:8", "openjdk version \"1.8.");
    }

    @Test
    void testDistributionWithJava11() throws IOException, URISyntaxException, InterruptedException {
        testWhiteRabbitInContainer("eclipse-temurin:11", "openjdk version \"11.0.");
    }
    @Test
    void testDistributionWithJava17() throws IOException, URISyntaxException, InterruptedException {
        testWhiteRabbitInContainer("eclipse-temurin:17", "openjdk version \"17.0.");
    }

    @Test
    @EnabledIfSystemProperty(named = "org.ohdsi.test.snowflake", matches = "(true|True|TRUE|yes|Yes|YES|1)")
    void testDistrubutionWithJava17AndSnowflake() {
        assertTrue(false, "should always trigger failure");
    }

    private void testWhiteRabbitInContainer(String imageName, String expectedVersion) throws IOException, InterruptedException, URISyntaxException {
        try (GenericContainer<?> javaContainer = createJavaContainer(imageName)) {
            javaContainer.start();

            Charset charset = StandardCharsets.UTF_8;
            Path iniFile = tempDir.resolve("tsv.ini");
            URL iniTemplate = VerifyDistributionIT.class.getClassLoader().getResource("scan_data/tsv.ini.template");
            URL referenceScanReport = VerifyDistributionIT.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-csv.xlsx");
            Path personCsv = Paths.get(VerifyDistributionIT.class.getClassLoader().getResource("scan_data/person-header.csv").toURI());
            Path costCsv = Paths.get(VerifyDistributionIT.class.getClassLoader().getResource("scan_data/cost-header.csv").toURI());
            assertNotNull(iniTemplate);
            String content = new String(Files.readAllBytes(Paths.get(iniTemplate.toURI())), charset);
            content = content.replaceAll("%WORKING_FOLDER%", WORKDIR_IN_CONTAINER);
            Files.write(iniFile, content.getBytes(charset));
            Files.copy(personCsv, tempDir.resolve("person.csv"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(costCsv, tempDir.resolve("cost.csv"), StandardCopyOption.REPLACE_EXISTING);

            // verify that the default java version in the container is actually 1.8
            ExecResult execResult = javaContainer.execInContainer("sh", "-c", "java -version");
            assertTrue(execResult.getStderr().startsWith(expectedVersion), "default java version in container should match version " + expectedVersion);

            // verify that the distribution of whiterabbit has been generated and is available inside the container
            execResult = javaContainer.execInContainer("sh", "-c", String.format("ls %s", APPDIR_IN_CONTAINER));
            assertTrue(execResult.getStdout().contains("repo"), "WhiteRabbit distribution is not accessible inside container");

            // run whiterabbit and verify the result
            execResult = javaContainer.execInContainer("sh", "-c", String.format("/app/bin/whiteRabbit -ini %s/tsv.ini", WORKDIR_IN_CONTAINER));
            assertTrue(execResult.getStdout().contains("Started new scan of 2 tables..."));
            assertTrue(execResult.getStdout().contains("Scanning table /whiterabbit/person.csv"));
            assertTrue(execResult.getStdout().contains("Scanning table /whiterabbit/cost.csv"));
            assertTrue(execResult.getStdout().contains("Scan report generated: /whiterabbit/ScanReport.xlsx"));

            assertTrue(ScanTestUtils.scanResultsSheetMatchesReference(tempDir.resolve("ScanReport.xlsx"), Paths.get(referenceScanReport.toURI()), DbType.DELIMITED_TEXT_FILES));

            javaContainer.stop();
        }
    }

    private GenericContainer<?> createJavaContainer(String imageName) {
        return new GenericContainer<>(
                DockerImageName.parse(imageName))
                .withCommand("sh", "-c", "tail -f /dev/null")
                .withFileSystemBind(Paths.get("../dist").toAbsolutePath().toString(), APPDIR_IN_CONTAINER)
                .withFileSystemBind(tempDir.toString(), WORKDIR_IN_CONTAINER, BindMode.READ_WRITE);

    }

    IniFile loadConfigurationFromEnv() {
        IniFile iniFile = new IniFile();

        return iniFile;
    }
}
