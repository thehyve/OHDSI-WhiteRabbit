package org.ohdsi.whiterabbit.scan;

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

public class TestSourceDataScanIniFileTsv {

    @Test
    void testSourceDataScan(@TempDir Path tempDir) throws URISyntaxException, IOException {
        Charset charset = StandardCharsets.UTF_8;
        Path iniFile = tempDir.resolve("tsv.ini");
        URL iniTemplate = TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/tsv.ini.template");
        Path personCsv = Paths.get(TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/person.csv").toURI());
        Path costCsv = Paths.get(TestSourceDataScanIniFileTsv.class.getClassLoader().getResource("scan_data/cost.csv").toURI());
        String content = new String(Files.readAllBytes(Paths.get(iniTemplate.toURI())), charset);
        content = content.replaceAll("%WORKING_FOLDER%", tempDir.toString());
        Files.write(iniFile, content.getBytes(charset));
        Files.copy(personCsv, tempDir.resolve("person.csv"));
        Files.copy(costCsv, tempDir.resolve("cost.csv"));
        WhiteRabbitMain wrMain = new WhiteRabbitMain(new String[]{"-ini", iniFile.toAbsolutePath().toString()});
        System.out.println("Hold it!");
        ScanTestUtils.verifyScanResultsFromXSLX(Paths.get(wrMain.reportFilePath), DbType.DELIMITED_TEXT_FILES);
        //sourceDataScan.process(dbSettings, outFile.toString());
        //ScanTestUtils.verifyScanResultsFromXSLX(outFile, dbSettings.dbType);
    }
}
