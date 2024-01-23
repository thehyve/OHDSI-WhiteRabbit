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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DBConnection;
import org.ohdsi.databases.MySqlHandler;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.databases.configuration.DbSettings;
import org.ohdsi.databases.configuration.DbType;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class SourceDataScanMySqlNewIT {

    @Container
    public static MySQLContainer<?> mySQLContainer = createMySQLContainer();

    @Test
    public void connectToDatabase() {
        // this is also implicitly tested by testSourceDataScan(), but having it fail separately helps identify problems quicker
        DBConnection dbConnection = connectWithTestConfiguration(MySqlHandler.INSTANCE);
        assertNotNull(dbConnection);
    }

    @Test
    public void testGetTableNames() {
        // this is also implicitly tested by testSourceDataScan(), but having it fail separately helps identify problems quicker
        connectWithTestConfiguration(MySqlHandler.INSTANCE);
        List<String> tableNames = MySqlHandler.INSTANCE.getTableNames();
        assertEquals(2, tableNames.size());
        assertTrue(tableNames.contains("cost"));
        assertTrue(tableNames.contains("person"));
    }

    public static MySQLContainer<?> createMySQLContainer() {
        MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.2")
                .withUsername("root")
                .withPassword("test")
                .withEnv("MYSQL_ROOT_PASSWORD", "test")
                .withDatabaseName("test")
                //.withReuse(true)
                .withClasspathResourceMapping(
                        "scan_data",
                        "/var/lib/mysql-files", // this is the directory configured in mysql to be accessible for scripts/files
                        BindMode.READ_ONLY)
                .withInitScript("scan_data/create_data_mysql.sql");

        mySQLContainer.start();

        return mySQLContainer;
    }

    @Test
    void testSourceDataScan(@TempDir Path tempDir) throws IOException, URISyntaxException {
        Path outFile = tempDir.resolve("scanresult.xslx");
        URL referenceScanReport = SourceDataScanMySqlNewIT.class.getClassLoader().getResource("scan_data/ScanReport-reference-v0.10.7-sql.xlsx");

        connectWithTestConfiguration(MySqlHandler.INSTANCE);
        SourceDataScan sourceDataScan = ScanTestUtils.createSourceDataScan();
        DbSettings dbSettings = getTestDbSettings();
        dbSettings.tables = getTableNames(dbSettings);

        sourceDataScan.process(dbSettings, outFile.toString());
        assertTrue(ScanTestUtils.scanResultsSheetMatchesReference(outFile, Paths.get(referenceScanReport.toURI()), DbType.MYSQL)); // DbType.MYSQL will do here
    }

    private List<String> getTableNames(DbSettings dbSettings) {
        try (RichConnection richConnection = new RichConnection(dbSettings)) {
            return richConnection.getTableNames(mySQLContainer.getDatabaseName());
        }
    }

    private DBConnection connectWithTestConfiguration(MySqlHandler mySqlHandler) {
        mySqlHandler.setConfiguration(
                mySQLContainer.getHost(),
                mySQLContainer.getFirstMappedPort().toString(),
                mySQLContainer.getDatabaseName(),
                mySQLContainer.getUsername(),
                mySQLContainer.getPassword());
        return mySqlHandler.getDBConnection();
    }

    private DbSettings getTestDbSettings() {
        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = DbType.MYSQL_NEW;
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;
        dbSettings.server = mySQLContainer.getHost() + ":" + mySQLContainer.getFirstMappedPort();
        dbSettings.database = mySQLContainer.getDatabaseName();
        dbSettings.user = mySQLContainer.getUsername();
        dbSettings.password = mySQLContainer.getPassword();
        //dbSettings.tables = getTableNames(dbSettings);

        return dbSettings;
    }
}
