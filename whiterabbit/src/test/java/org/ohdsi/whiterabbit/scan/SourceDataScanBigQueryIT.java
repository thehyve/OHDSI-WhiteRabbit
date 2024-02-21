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
package org.ohdsi.whiterabbit.scan;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.databases.configuration.DbSettings;
import org.ohdsi.databases.configuration.DbType;
import org.testcontainers.containers.BigQueryEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceDataScanBigQueryIT {

    @BeforeAll
    public static void startContainer() {
    }

//    @Test
//    public void connectToDatabase() {
//        // this is also implicitly tested by testSourceDataScan(), but having it fail separately helps identify problems quicker
//        DbSettings dbSettings = getTestDbSettings();
//        try (RichConnection richConnection = new RichConnection(dbSettings)) {
//            // do nothing, connection will be closed automatically because RichConnection implements interface Closeable
//        }
//    }
//
//    @Test
//    public void testGetTableNames() {
//        // this is also implicitly tested by testSourceDataScan(), but having it fail separately helps identify problems quicker
//        DbSettings dbSettings = getTestDbSettings();
//        List<String> tableNames = getTableNames(dbSettings);
//        assertEquals(2, tableNames.size());
//    }

    @Test
    void testSourceDataScan(@TempDir Path tempDir) throws IOException, URISyntaxException {
        Path outFile = tempDir.resolve("scanresult.xlsx");
        URL referenceScanReport = SourceDataScanBigQueryIT.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-sql.xlsx");

        SourceDataScan sourceDataScan = ScanTestUtils.createSourceDataScan();
        DbSettings dbSettings = getTestDbSettings();

        sourceDataScan.process(dbSettings, outFile.toString());

        Files.copy(outFile, Paths.get("/var/tmp/scanresults-bigquery.xlsx"), StandardCopyOption.REPLACE_EXISTING);
        assertTrue(ScanTestUtils.scanResultsSheetMatchesReference(outFile, Paths.get(referenceScanReport.toURI()), DbType.BIGQUERY));
    }

    private List<String> getTableNames(DbSettings dbSettings) {
        try (RichConnection richConnection = new RichConnection(dbSettings)) {
            return richConnection.getTableNames("OHDSI");
        }
    }

    private DbSettings getTestDbSettings() {
        // TODO get settings from bigquery.env file
        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = DbType.BIGQUERY;
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;
        dbSettings.server = "elite-thunder-415014";
        dbSettings.user = "";   // TODO get from bigquery.env file
        dbSettings.password = "";   // TODO get from bigquery.env file
        dbSettings.tables = getTableNames(dbSettings);
        dbSettings.database = "OHDSI";
        dbSettings.domain = "OHDSI";
        dbSettings.schema = "OHDSI";

        return dbSettings;
    }
}
