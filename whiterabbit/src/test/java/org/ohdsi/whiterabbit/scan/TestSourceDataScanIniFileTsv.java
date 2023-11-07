package org.ohdsi.whiterabbit.scan;

import org.opentest4j.AssertionFailedError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbType;
import org.ohdsi.whiteRabbit.WhiteRabbitMain;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TestSourceDataScanIniFileTsv {

    @Test
    void testSourceDataScan(@TempDir Path tempDir) throws URISyntaxException, IOException {
        Charset charset = StandardCharsets.UTF_8;
        Path iniFile = tempDir.resolve("tsv.ini");
        URL iniTemplate = TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/tsv.ini.template");
        URL referenceScanReport = TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-csv.xlsx");
        Path personCsv = Paths.get(TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/person-header.csv").toURI());
        Path costCsv = Paths.get(TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/cost-header.csv").toURI());
        assert iniTemplate != null;
        String content = new String(Files.readAllBytes(Paths.get(iniTemplate.toURI())), charset);
        content = content.replaceAll("%WORKING_FOLDER%", tempDir.toString());
        Files.write(iniFile, content.getBytes(charset));
        Files.copy(personCsv, tempDir.resolve("person.csv"));
        Files.copy(costCsv, tempDir.resolve("cost.csv"));
        WhiteRabbitMain wrMain = new WhiteRabbitMain(new String[]{"-ini", iniFile.toAbsolutePath().toString()});
        System.out.println("Hold it!");
        assert referenceScanReport != null;
        ScanTestUtils.compareScanResultsToReference(tempDir.resolve("ScanReport.xlsx"), Paths.get(referenceScanReport.toURI()), DbType.POSTGRESQL);
    }


    @Test
    // minimal test to verify comparing ScanReports: test the tester :-)
    void testCompareSheets() throws URISyntaxException, IOException {
        Map<String, List<List<String>>> sheets1 = Collections.singletonMap("Field Overview", Collections.singletonList(Arrays.asList("one", "two", "three")));
        Map<String, List<List<String>>> sheets2 = Collections.singletonMap("Field Overview", Collections.singletonList(Arrays.asList("one", "two", "three")));
        Map<String, List<List<String>>> sheets3 = Collections.singletonMap("Field Overview", Collections.singletonList(Arrays.asList("two", "three", "four")));
        AssertionFailedError thrown = Assertions.assertThrows(AssertionFailedError.class, () -> {
            ScanTestUtils.compareSheets(sheets1, sheets3, DbType.POSTGRESQL);
        }, "AssertionFailedError was expected");
        ScanTestUtils.compareSheets(sheets1, sheets2, DbType.POSTGRESQL);
    }
}
