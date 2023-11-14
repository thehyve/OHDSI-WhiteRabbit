package org.ohdsi.whiterabbit.scan;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.whiteRabbit.Console;
import org.ohdsi.whiteRabbit.WhiteRabbitMain;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ohdsi.databases.DbType.POSTGRESQL;
import static org.ohdsi.whiterabbit.scan.SourceDataScanPostgreSQLIT.createPostgreSQLContainer;

@ExtendWith(GUITestExtension.class)
@CacioTest
class SourceDataScanPostgreSQLGuiIT {

    private static FrameFixture window;
    private static Console console;

    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;
    @BeforeAll
    public static void setupOnce() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    @BeforeEach
    public void onSetUp() {
        String[] args = {};
        WhiteRabbitMain whiteRabbitMain = GuiActionRunner.execute(() -> new WhiteRabbitMain(true, args));
        console = whiteRabbitMain.getConsole();
        window = new FrameFixture(whiteRabbitMain.getFrame());
        window.show(); // shows the frame to test
    }

    @Container
    public static PostgreSQLContainer<?> postgreSQL = createPostgreSQLContainer();

    @GUITest
    @Test
    void testSourceDataScan(@TempDir Path tempDir) throws IOException, URISyntaxException {
        window.show(); // shows the frame to test
        URL referenceScanReport = TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-sql.xlsx");
        Path personCsv = Paths.get(TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/person-no-header.csv").toURI());
        Path costCsv = Paths.get(TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/cost-no-header.csv").toURI());
        Files.copy(personCsv, tempDir.resolve("person.csv"));
        Files.copy(costCsv, tempDir.resolve("cost.csv"));
        window.tabbedPane(WhiteRabbitMain.NAME_TABBED_PANE).selectTab(WhiteRabbitMain.LABEL_LOCATIONS);
        window.comboBox("SourceType").selectItem(WhiteRabbitMain.DBChoice.PostgreSQL.toString());
        window.textBox("FolderField").setText(tempDir.toAbsolutePath().toString());
        window.textBox(WhiteRabbitMain.LABEL_SERVER_LOCATION).setText(String.format("%s:%s/%s",
                postgreSQL.getHost(),
                postgreSQL.getFirstMappedPort(),
                postgreSQL.getDatabaseName()));
        window.textBox(WhiteRabbitMain.LABEL_USER_NAME).setText(postgreSQL.getUsername());
        window.textBox(WhiteRabbitMain.LABEL_PASSWORD).setText(postgreSQL.getPassword());
        window.textBox(WhiteRabbitMain.LABEL_DATABASE_NAME).setText("public");
        window.button(WhiteRabbitMain.LABEL_TEST_CONNECTION).click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class, true) {
            protected boolean isMatching(JDialog frame) {
                return WhiteRabbitMain.LABEL_CONNECTION_SUCCESSFUL.equals(frame.getTitle());
            }
        };
        DialogFixture frame = WindowFinder.findDialog(matcher).using(window.robot());
        frame.button().click();

        window.tabbedPane(WhiteRabbitMain.NAME_TABBED_PANE).selectTab(WhiteRabbitMain.LABEL_SCAN).click();
        window.button(WhiteRabbitMain.LABEL_ADD_ALL_IN_DB).click();
        window.button(WhiteRabbitMain.LABEL_SCAN_TABLES).click();

        assertTrue(ScanTestUtils.isScanReportGeneratedAndMatchesReference(
                console,
                tempDir.resolve("ScanReport.xlsx"),
                Paths.get(referenceScanReport.toURI()),
                POSTGRESQL));
    }
}
