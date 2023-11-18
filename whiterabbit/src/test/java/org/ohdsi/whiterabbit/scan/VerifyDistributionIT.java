package org.ohdsi.whiterabbit.scan;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbType;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Intent: "deploy" the distributed application in a docker container containing a Java 8 runtime,
 * and run a set of tests that aim to verify that the distribution is complete, i.e. no dependencies
 * are missing. A data for a scan on csv files is used to run whiterabbit.
 *
 * Note that this does not test any of the JDBC driver dependencies
 */
public class VerifyDistributionIT {

    @TempDir
    static Path tempDir;

    private static final String WORKDIR_IN_CONTAINER = "/whiterabbit";
    private static final String APPDIR_IN_CONTAINER = "/app";
    @Container
    static GenericContainer<?> java8Container;

    static GenericContainer<?> createJava8Container() {
        // the container should have java 8, the generated whiterabbit distribution should be available, and
        // the workdir of the running test should be availabe (through file system binds)
        return new GenericContainer<>(
                DockerImageName.parse("eclipse-temurin:8"))
                .withCommand("sh", "-c", "tail -f /dev/null")
                .withFileSystemBind(Paths.get("../dist").toAbsolutePath().toString(), APPDIR_IN_CONTAINER)
                .withFileSystemBind(tempDir.toString(), WORKDIR_IN_CONTAINER, BindMode.READ_WRITE);
    }

    @BeforeAll
    public static void beforeAll() {
        java8Container = createJava8Container();
        try {
            java8Container.start();
        } catch (ContainerLaunchException | TimeoutException e) {
            String logs = java8Container.getLogs();
            System.out.println(logs);
        }
    }

    @AfterAll
    public static void afterAll() {
        java8Container.stop();
    }

    @Test
    public void testWhiteRabbitInContainer() throws IOException, InterruptedException, URISyntaxException {
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
        Files.copy(personCsv, tempDir.resolve("person.csv"));
        Files.copy(costCsv, tempDir.resolve("cost.csv"));

        // verify that the default java version in the container is actually 1.8
        ExecResult execResult = java8Container.execInContainer("sh", "-c", "java -version");
        assertTrue(execResult.getStderr().startsWith("openjdk version \"1.8.0_"));

        // verify that the distribution of whiterabbit has been generated and is available inside the container
        execResult = java8Container.execInContainer("sh", "-c", String.format("ls %s", APPDIR_IN_CONTAINER));
        assertTrue(execResult.getStdout().contains("repo"));

        // run whiterabbit and verify the result
        execResult = java8Container.execInContainer("sh", "-c", String.format("/app/bin/whiteRabbit -ini %s/tsv.ini", WORKDIR_IN_CONTAINER));
        assertTrue(ScanTestUtils.scanResultsSheetMatchesReference(tempDir.resolve("ScanReport.xlsx"), Paths.get(referenceScanReport.toURI()), DbType.DELIMITED_TEXT_FILES));
    }
}
