package org.ohdsi.whiterabbit.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.DbSettings;
import org.ohdsi.databases.DbType;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.whiteRabbit.scan.SourceDataScan;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class SourceDataScanPostgreSQLIT {

    @Container
    public static PostgreSQLContainer<?> postgreSQL = createPostgreSQLContainer();

    @Test
    public void connectToDatabase() {
        // this is also implicitly tested by testSourceDataScan(), but having it fail separately helps identify problems quicker
        DbSettings dbSettings = getTestDbSettings();
        try (RichConnection richConnection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType)) {
            // do nothing, connection will be closed automatically because RichConnection implements interface Closeable
        }
    }

    @Test
    public void testGetTableNames() {
        // this is also implicitly tested by testSourceDataScan(), but having it fail separately helps identify problems quicker
        DbSettings dbSettings = getTestDbSettings();
        List<String> tableNames = getTableNames(dbSettings);
        assertEquals(2, tableNames.size());
    }

    public static PostgreSQLContainer<?> createPostgreSQLContainer() {
        PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13.1")
                .withUsername("test")
                .withPassword("test")
                .withDatabaseName("test")
                .withClasspathResourceMapping(
                        "scan_data",
                        "/scan_data",
                        BindMode.READ_ONLY)
                .withInitScript("scan_data/create_data_postgresql.sql");

        postgreSQLContainer.start();

        return postgreSQLContainer;
    }

    @Test
    void testSourceDataScan(@TempDir Path tempDir) throws IOException {
        Path outFile = tempDir.resolve("scanresult.xslx");
        SourceDataScan sourceDataScan = new SourceDataScan();
        DbSettings dbSettings = getTestDbSettings();

        sourceDataScan.process(dbSettings, outFile.toString());
        ScanTestUtils.verifyScanResultsFromXSLX(outFile, dbSettings.dbType);
    }

    private List<String> getTableNames(DbSettings dbSettings) {
        try (RichConnection richConnection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType)) {
            return richConnection.getTableNames("public");
        }
    }

    private DbSettings getTestDbSettings() {
        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = DbType.POSTGRESQL;
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;
        dbSettings.server = postgreSQL.getJdbcUrl();
        dbSettings.database = "public"; // always for PostgreSQL
        dbSettings.user = postgreSQL.getUsername();
        dbSettings.password = postgreSQL.getPassword();
        dbSettings.tables = getTableNames(dbSettings);

        return dbSettings;
    }
}
