package org.ohdsi.whiterabbit.scan;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbType;
import org.ohdsi.whiteRabbit.WhiteRabbitMain;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ohdsi.whiteRabbit.WhiteRabbitMain.DELIMITED_TEXT_FILES;

@CacioTest
public class TestSourceDataScanCsvGui {
    private static FrameFixture window;

    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;
    @BeforeAll
    public static void setupOnce() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    @BeforeEach
    public void setUp() {
        String[] args = {};
        WhiteRabbitMain whiteRabbitMain = GuiActionRunner.execute(() -> new WhiteRabbitMain(args));
        window = new FrameFixture(whiteRabbitMain.getFrame());
        window.show(); // shows the frame to test
    }

    @GUITest
    @Test
    void testSourceDataScanFromGui(@TempDir Path tempDir) throws IOException, URISyntaxException {
        URL referenceScanReport = TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-csv.xlsx");
        Path personCsv = Paths.get(TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/person-header.csv").toURI());
        Path costCsv = Paths.get(TestSourceDataScanCsvGui.class.getClassLoader().getResource("scan_data/cost-header.csv").toURI());
        Files.copy(personCsv, tempDir.resolve("person.csv"));
        Files.copy(costCsv, tempDir.resolve("cost.csv"));
        window.tabbedPane("TabbedPane").selectTab("Locations");
        window.comboBox("SourceType").selectItem(DELIMITED_TEXT_FILES);
        window.textBox("Delimiter").setText(",");
        window.textBox("FolderField").setText(tempDir.toAbsolutePath().toString());
        window.tabbedPane("TabbedPane").selectTab("Scan");
        window.button("Add").click();
        window.fileChooser("FileChooser").fileNameTextBox().setText("\"cost.csv\" \"person.csv\"");
        window.fileChooser("FileChooser").approveButton().click();
        window.button("ScanTables").click();
        window.textBox("Console");

        // wait for the "Scan report generated:" message in the Console text area
        pause(new Condition("Label Timeout") {
            public boolean test() {
                return window.textBox("Console").text().contains("Scan report generated:");
            }

        }, timeout(10000));
        assertTrue(window.textBox("Console").text().contains("Scan report generated:"));
        assert referenceScanReport != null;
        ScanTestUtils.compareScanResultsToReference(tempDir.resolve("ScanReport.xlsx"), Paths.get(referenceScanReport.toURI()), DbType.DELIMITED_TEXT_FILES);
    }
}
