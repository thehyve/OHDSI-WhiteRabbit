package org.ohdsi.whiterabbit.scan;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.SnowflakeConnector;
import org.ohdsi.databases.SnowflakeTestUtils;
import org.ohdsi.databases.DbType;
import org.ohdsi.whiterabbit.Console;
import org.ohdsi.whiterabbit.WhiteRabbitMain;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ohdsi.databases.DbType.SNOWFLAKE;
import static org.ohdsi.whiterabbit.scan.SourceDataScanSnowflakeIT.*;

@ExtendWith(GUITestExtension.class)
@CacioTest
class SourceDataScanSnowflakeGuiIT {

    private static FrameFixture window;
    private static Console console;

    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;
    @BeforeAll
    public static void setupOnce() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    @Container
    public static GenericContainer<?> testContainer;

    @BeforeEach
    public void onSetUp() {
        try {
            testContainer = createPythonContainer();
            prepareTestData();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Creating python container failed.");
        }
        String[] args = {};
        WhiteRabbitMain whiteRabbitMain = GuiActionRunner.execute(() -> new WhiteRabbitMain(true, args));
        console = whiteRabbitMain.getConsole();
        window = new FrameFixture(whiteRabbitMain.getFrame());
        window.show(); // shows the frame to test
    }

    @GUITest
    @Test
    @EnabledIfEnvironmentVariable(named = SNOWFLAKE_ACCOUNT_ENVIRONMENT_VARIABLE, matches = ".+")
    void testConnectionAndSourceDataScan(@TempDir Path tempDir) throws IOException, URISyntaxException {
        URL referenceScanReport = TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-sql.xlsx");
        Path personCsv = Paths.get(TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/person-no-header.csv").toURI());
        Path costCsv = Paths.get(TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/cost-no-header.csv").toURI());
        Files.copy(personCsv, tempDir.resolve("person.csv"));
        Files.copy(costCsv, tempDir.resolve("cost.csv"));
        window.tabbedPane(WhiteRabbitMain.NAME_TABBED_PANE).selectTab(WhiteRabbitMain.LABEL_LOCATIONS);
        window.comboBox("SourceType").selectItem(DbType.SNOWFLAKE.label());
        window.textBox("FolderField").setText(tempDir.toAbsolutePath().toString());
        window.textBox(SnowflakeConnector.SnowflakeConfiguration.SNOWFLAKE_ACCOUNT).setText(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_ACCOUNT"));
        window.textBox(SnowflakeConnector.SnowflakeConfiguration.SNOWFLAKE_USER).setText(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_USER"));
        window.textBox(SnowflakeConnector.SnowflakeConfiguration.SNOWFLAKE_PASSWORD).setText(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_PASSWORD"));
        window.textBox(SnowflakeConnector.SnowflakeConfiguration.SNOWFLAKE_WAREHOUSE).setText(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_WAREHOUSE"));
        window.textBox(SnowflakeConnector.SnowflakeConfiguration.SNOWFLAKE_DATABASE).setText(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_DATABASE"));
        window.textBox(SnowflakeConnector.SnowflakeConfiguration.SNOWFLAKE_SCHEMA).setText(SnowflakeTestUtils.getenvOrFail("SNOWFLAKE_WR_TEST_SCHEMA"));

        // use the "Test connection" button
        window.button(WhiteRabbitMain.LABEL_TEST_CONNECTION).click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class, true) {
            protected boolean isMatching(JDialog frame) {
                return WhiteRabbitMain.LABEL_CONNECTION_SUCCESSFUL.equals(frame.getTitle());
            }
        };
        DialogFixture frame = WindowFinder.findDialog(matcher).using(window.robot());
        frame.button().click();

        // switch to the scan panel, add all tables found and run the scan
        window.tabbedPane(WhiteRabbitMain.NAME_TABBED_PANE).selectTab(WhiteRabbitMain.LABEL_SCAN).click();
        window.button(WhiteRabbitMain.LABEL_ADD_ALL_IN_DB).click();
        window.button(WhiteRabbitMain.LABEL_SCAN_TABLES).click();

        // verify the generated scan report against the reference
        assertTrue(ScanTestUtils.isScanReportGeneratedAndMatchesReference(
                console,
                tempDir.resolve("ScanReport.xlsx"),
                Paths.get(referenceScanReport.toURI()),
                SNOWFLAKE));
    }
}
