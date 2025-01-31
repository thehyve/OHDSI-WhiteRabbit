/*******************************************************************************
 * Copyright 2023 Observational Health Data Sciences and Informatics & The Hyve
 *
 * This file is part of WhiteRabbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.rabbitInAHat;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.ComponentDragAndDrop;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ActionFailedException;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.timing.Condition;
import org.junit.jupiter.api.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.junit.Assert.*;
import static org.ohdsi.rabbitInAHat.MaskListDialog.*;
import static org.ohdsi.rabbitInAHat.RabbitInAHatMain.*;

/*
 * The @CacioTest annotation below  enables running the Swing GUI tests in a virtual screen. This allows the integration tests to run
 * anywhere without being blocked by the absence of a real screen (e.g. github actions), and without being
 * disrupted by unrelated user activity on workstations/laptops (any keyboard or mouse action).
 * For debugging purposes, you can disable the annotation below to have the tests run on your screen. Be aware that
 * any interaction with mouse or keyboard can (will) disrupt the tests if they run on your screen.
 * Also keep in mind that the tests may fail if your screen has different dimensions than the virtual screen (as defined by
 * VIRTUAL_SCREEN_WIDTH and VIRTUAL_SCREEN_HEIGHT below).
 */
@CacioTest

public class RabbitInAHatIT {

    private static FrameFixture window;

    private final static int VIRTUAL_SCREEN_WIDTH = 1920;
    private final static int VIRTUAL_SCREEN_HEIGHT = 1080;
    @BeforeAll
    public static void setupOnce() throws IOException {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", VIRTUAL_SCREEN_WIDTH, VIRTUAL_SCREEN_HEIGHT));
        // unzip examples.zip into target resources
        Path examplesPath = Paths.get("examples.zip");
        if (!Files.exists(examplesPath)) {
            examplesPath = Paths.get("../examples.zip");
        }
        RiahTestUtils.unzip(examplesPath, Paths.get("target/test-classes").toAbsolutePath());
    }

    @BeforeEach
    public void setUp() {
        String[] args = {};
        RabbitInAHatMain rabbitInAHatMain = GuiActionRunner.execute(() -> new RabbitInAHatMain(args));
        window = new FrameFixture(rabbitInAHatMain.getFrame());
        window.splitPane("splitpane").target().setDividerLocation(VIRTUAL_SCREEN_WIDTH / 2);
        // window.robot().settings().delayBetweenEvents(150); // increase delay between events to make it easier to follow the tests
        window.show(); // shows the frame to test
    }

    @AfterEach

    public void tearDown() {
        // uncertain if the assertions with isActive() are sufficient, but at least it's an indication the open window
        // did close
        if (window.target().isActive()) {
            window.menuItem(ACTION_EXIT).click();                   // select Exit from menu
            window.optionPane().buttonWithText("Yes").click();      // confirm that we want to exit
            assertFalse(window.target().isActive());                // main window is now inactive
        }
        window.robot().cleanUp();                                   // otherwise subsequent tests will get stuck
    }

    @Test
    public void openApp() {
        // this just opens and closes the application. No point in trying anything else if this does not work
        assertTrue(true);
    }

    @Test
    void openScanReportAndAddStemTable() throws URISyntaxException {
        openScanReport("examples/test_scanreports/ScanReport_minimal.xlsx");
        MappingPanel tablesPanel = getTablesPanel();

        window.menuItem(ACTION_ADD_STEM_TABLE).click();

        verifyMapping(tablesPanel, "stem_table", "condition_occurrence");
        verifyMapping(tablesPanel, "stem_table", "drug_exposure");
        verifyMapping(tablesPanel, "stem_table", "procedure_occurrence");
        verifyMapping(tablesPanel, "stem_table", "device_exposure");
        verifyMapping(tablesPanel, "stem_table", "measurement");
        verifyMapping(tablesPanel, "stem_table", "observation");
        // there are more mappings, but these may not be visible depending on the actual screen size
    }

    @Test
    public void openAndVerifySavedETLSpecs() throws URISyntaxException {
        // open the test ETL specification
        openETLSpecs("scan/etl-specs.json.gz");
        MappingPanel tablesPanel = getTablesPanel();

        assertEquals("7 mappings are expected", 7, tablesPanel.getArrows().size());

        // verify the mappings
        verifyMapping(tablesPanel, "patients.csv", "person");
        verifyMapping(tablesPanel, "claims.csv", "person");
        verifyMapping(tablesPanel, "conditions.csv", "observation");
        verifyMapping(tablesPanel, "conditions.csv", "condition_occurrence");
        verifyMapping(tablesPanel, "medications.csv", "drug_exposure");
        verifyMapping(tablesPanel, "encounters.csv", "observation_period");
        verifyMapping(tablesPanel, "encounters.csv", "visit_occurrence");
    }

    @Test
    void openSavedETLSpecsAndSelectMapping() throws URISyntaxException, InterruptedException {
        // open the test ETL specification
        openETLSpecs("scan/etl-specs.json.gz");
        MappingPanel tablesPanel = getTablesPanel();

        final String sourceName = "patients.csv";
        final String targetName = "person";
        LabeledRectangle sourceTable = findMappableItem(tablesPanel.getVisibleSourceComponents(), sourceName);
        LabeledRectangle targetTable = findMappableItem(tablesPanel.getVisibleTargetComponents(), targetName);
        assertFalse(sourceTable.isSelected());
        assertFalse(targetTable.isSelected());

        Arrow mapping = findMapping(tablesPanel.getArrows(), sourceName, targetName);

        // double click the mapping (arrow)
        window.robot().click(tablesPanel, mapping.getPointInside(), MouseButton.LEFT_BUTTON, 2);
        MappingPanel fieldsPanel = getPanel(PANEL_FIELD_MAPPING);
        pause(new Condition("wait for source items to appear in the items panel") {
            public boolean test() {
                return fieldsPanel.getVisibleSourceComponents().size() != 0 &&
                        fieldsPanel.getVisibleSourceComponents().get(0).getItem().getName().equals("id");
            }
        }, timeout(10000));

        // verify the mappings
        assertEquals("incorrect numbers of mappings", 11, fieldsPanel.getArrows().size());
        verifyMapping(fieldsPanel, "id", "person_source_value");
        verifyMapping(fieldsPanel, "birthdate", "year_of_birth");
        verifyMapping(fieldsPanel, "birthdate", "month_of_birth");
        verifyMapping(fieldsPanel, "birthdate", "day_of_birth");
        verifyMapping(fieldsPanel, "birthdate", "birth_datetime");
        verifyMapping(fieldsPanel, "race", "race_concept_id");
        verifyMapping(fieldsPanel, "race", "race_source_value");
        /* more could be verified but these fall outside the "visible" screen when running with @CacioTest
        verifyMapping(fieldsPanel, "ethnicity", "ethnicity_concept_id");
        verifyMapping(fieldsPanel, "ethnicity", "ethnicity_source_value");
        verifyMapping(fieldsPanel, "gender", "gender_concept_id");
        verifyMapping(fieldsPanel, "gender", "gender_source_value"); */
    }

    @GUITest
    @Test
    public void openAndVerifySavedETLSpecsWithHiddenTables() throws URISyntaxException {
        // open the test ETL specification
        openETLSpecs("scan/etl-specs.json.gz");
        MappingPanel tablesPanel = getTablesPanel();

        hideTables();

        assertEquals("4 mappings are expected", 4, tablesPanel.getArrows().size());

        // verify the mappings
        verifyMapping(tablesPanel, "conditions.csv", "observation");
        verifyMapping(tablesPanel, "conditions.csv", "condition_occurrence");
        verifyMapping(tablesPanel, "encounters.csv", "observation_period");
        verifyMapping(tablesPanel, "encounters.csv", "visit_occurrence");
    }

    private void hideTables() throws URISyntaxException {
        window.menuItem(ACTION_HIDE_TABLES).click();
        DialogFixture hideTablesDialog = window.dialog(MASK_LIST_DIALOG);
        hideTablesDialog.moveTo(new Point(1, 1));  // avoid problems with the window being out of the boundaries of the "visible" screen
        hideTablesDialog.textBox().setText(".csv");
        hideTablesDialog.button(DESELECT_BUTTON).click();
        hideTablesDialog.radioButton(REGEX_BUTTON).click();
        hideTablesDialog.textBox().setText(".*co.*");
        hideTablesDialog.button(SELECT_BUTTON).click();
        hideTablesDialog.close();
    }


    @GUITest
    @Test
    public void createTableMapping() throws URISyntaxException {
        openETLSpecs("scan/etl-specs.json.gz");
        MappingPanel tablesPanel = getTablesPanel();
        createAndVerifyTableMapping(tablesPanel, "devices.csv", "device_exposure");
    }

    private void openETLSpecs(String specName) throws URISyntaxException {
        window.menuItem(ACTION_OPEN_ETL_SPECS).click();
        JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(window.robot());
        assertEquals(TITLE_SELECT_FILE, fileChooser.target().getDialogTitle());
        URL etlSpecsUrl = this.getClass().getClassLoader().getResource(specName);
        File specs = new File(Objects.requireNonNull(etlSpecsUrl).toURI());
        fileChooser.setCurrentDirectory(specs.getParentFile());
        fileChooser.selectFile(specs).approve();
        MappingPanel tablesPanel = window.panel(PANEL_TABLE_MAPPING).targetCastedTo(MappingPanel.class);
        pause(new Condition("wait for source items to appear in the tables panel") {
            public boolean test() {
                return !tablesPanel.getVisibleSourceComponents().isEmpty();
            }
        }, timeout(10000));
        assertFalse("There should be source items", tablesPanel.getVisibleSourceComponents().isEmpty());
        assertFalse("There should be target items", tablesPanel.getVisibleTargetComponents().isEmpty());
    }

    private void openScanReport(String reportPath) throws URISyntaxException {
        window.menuItem(ACTION_OPEN_SCAN_REPORT).click();
        JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(window.robot());
        assertEquals(TITLE_SELECT_FILE, fileChooser.target().getDialogTitle());
        URL scanReportUrl = RabbitInAHatIT.class.getClassLoader().getResource(reportPath);
        File scanReport = new File(Objects.requireNonNull(scanReportUrl).toURI());
        fileChooser.setCurrentDirectory(scanReport.getParentFile());
        fileChooser.selectFile(scanReport).approve();
        //fileChooser.selectFile(new File(Objects.requireNonNull(scanReportUrl).toURI())).approve();
    }


    private void verifyMapping(MappingPanel tablesPanel, String sourceName, String targetName, String... details) {
        LabeledRectangle sourceTable = findMappableItem(tablesPanel.getVisibleSourceComponents(), sourceName);
        LabeledRectangle targetTable = findMappableItem(tablesPanel.getVisibleTargetComponents(), targetName);
        assertFalse(sourceTable.isSelected());
        assertFalse(targetTable.isSelected());

        Arrow mapping = findMapping(tablesPanel.getArrows(), sourceName, targetName);

        assertEquals(Arrow.HighlightStatus.NONE_SELECTED, mapping.getHighlightStatus());
        clickAndVerifyLabeledRectangles(tablesPanel, sourceTable);
        assertEquals(Arrow.HighlightStatus.SOURCE_SELECTED, mapping.getHighlightStatus());

        clickAndVerifyLabeledRectangles(tablesPanel, targetTable);
        assertEquals(Arrow.HighlightStatus.TARGET_SELECTED, mapping.getHighlightStatus());

        deselectAll(tablesPanel);
        clickAndVerifyLabeledRectangles(tablesPanel, sourceTable, targetTable);
        assertEquals(Arrow.HighlightStatus.BOTH_SELECTED, mapping.getHighlightStatus());

        deselectAll(tablesPanel);
    }

    private void clickAndVerifyLabeledRectangles(MappingPanel tablesPanel, LabeledRectangle... rectangles) {
        Arrays.stream(rectangles).forEach(r -> {
            assertFalse(r.isSelected());
            if (rectangles.length > 1) {
                window.robot().pressKey(KeyEvent.VK_SHIFT);
            }
            Point pointToClick = new Point(r.getX() + 1, r.getY() + 1);
            try {
                window.robot().click(tablesPanel, pointToClick);
            } catch (ActionFailedException e) {
                System.out.println("Failed to click on " + r.getItem().getName() + " at " + pointToClick);
                //e.printStackTrace();
            }
            if (rectangles.length > 1) {
                window.robot().releaseKey(KeyEvent.VK_SHIFT);
            }
            assertTrue(r.isSelected());
        });
    }

    LabeledRectangle findMappableItem(List<LabeledRectangle> items, String name) {
        LabeledRectangle[] matchingItems = items.stream().filter(i -> i.getItem().getName().equals(name)).toArray(LabeledRectangle[]::new);
        assertEquals(String.format("There should be exactly 1 item with name %s", name), 1, matchingItems.length);

        return matchingItems[0];
    }

    Arrow findMapping(List<Arrow> mappings, String source, String target) {
        Arrow[] matchingMappings = mappings.stream().filter(m -> m.getSource().getItem().getName().equals(source) &&
                m.getTarget().getItem().getName().equals(target)).toArray(Arrow[]::new);
        assertEquals(String.format("Mapping from %s to %s is expected to exists", source, target),
                1, matchingMappings.length);

        return matchingMappings[0];
    }

    private void deselectAll(MappingPanel tablesPanel) {
        tablesPanel.getVisibleSourceComponents().forEach(l -> l.setSelected(false));
        tablesPanel.getVisibleTargetComponents().forEach(l -> l.setSelected(false));
        tablesPanel.getArrows().forEach(a -> a.setSelected(false));
    }

    private void createAndVerifyTableMapping(MappingPanel tablesPanel, String source, String target) {
        // pre: source and target must not be connected
        assertEquals(String.format("A mapping between source '%s' and target '%s' should not yet exist.", source, target),
                0,
                 tablesPanel.getArrows()
                         .stream()
                         .filter(a -> a.getSource().getItem().getName().equalsIgnoreCase(source) &&
                                      a.getTarget().getItem().getName().equalsIgnoreCase(target)).count());

        // action: drag the arrowhead at sourceItem to targetItem
        LabeledRectangle sourceItem = findMappableItem(tablesPanel.getVisibleSourceComponents(), source);
        LabeledRectangle targetItem = findMappableItem(tablesPanel.getVisibleTargetComponents(), target);

        ComponentDragAndDrop dragAndDrop = new ComponentDragAndDrop(window.robot());
        dragAndDrop.drag(tablesPanel, arrowHeadLocation(sourceItem));
        dragAndDrop.drop(tablesPanel, new Point(targetItem.getX(), targetItem.getY()));

        // post: there should be a mapping between source and target
        verifyMapping(tablesPanel, source, target);
    }

    private Point arrowHeadLocation(LabeledRectangle item) {
        return new Point(item.getX() + item.getWidth() + (Arrow.headThickness / 2), item.getY() + item.getHeight() / 2);
    }

    private MappingPanel getPanel(String name) {
        return window.panel(name).targetCastedTo(MappingPanel.class);
    }

    private MappingPanel getTablesPanel() {
        return window.panel(PANEL_TABLE_MAPPING).targetCastedTo(MappingPanel.class);
    }

}