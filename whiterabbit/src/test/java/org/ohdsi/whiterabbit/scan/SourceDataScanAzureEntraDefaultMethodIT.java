package org.ohdsi.whiterabbit.scan;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.databases.AzureSqlEntraHandler;
import org.ohdsi.databases.configuration.DbType;
import org.ohdsi.whiterabbit.WhiteRabbitMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for scanning Azure SQL using Entra ID authentication.
 *
 * Credentials are provided via sqlserver-azure-entra.env which is loaded by ScanTestUtils.PropertiesFileChecker.
 */
@ExtendWith(SystemStubsExtension.class)
public class SourceDataScanAzureEntraDefaultMethodIT {
    @SystemStub
    private EnvironmentVariables environmentVariables;
    Logger logger = LoggerFactory.getLogger(SourceDataScanAzureEntraDefaultMethodIT.class);

    @BeforeEach
    void beforeEach() {
        Assumptions.assumeTrue(new ScanTestUtils.PropertiesFileChecker("sqlserver-azure-entra-default.env"),
                "Azure Entra system default method properties file not available");
        
        environmentVariables.set("AZURE_TENANT_ID", ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_TENANT_ID"));
        environmentVariables.set("AZURE_CLIENT_ID", ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_CLIENT_ID"));
        environmentVariables.set("AZURE_CLIENT_SECRET", ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_CLIENT_SECRET"));
    }

    @Test
    void testProcessAzureEntraDefaultMethodFromIni(@TempDir Path tempDir) throws URISyntaxException, IOException {
//        Assumptions.assumeTrue(new ScanTestUtils.PropertiesFileChecker("sqlserver-azure-entra-default.env"),
//                "Azure Entra system default method properties file not available");
        System.out.println("SLF4J is using logger implementation: " + logger.getClass().getName());
        logger.info("Attempting Azure Entra ID based authentication with credentials provided by the environment");

        // first verify that the manipulation of environment variables did it's thing
        assert(System.getenv("AZURE_TENANT_ID")).equals(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_TENANT_ID"));

        // Build an ini file for WhiteRabbit to run the scan
        Path iniFile = tempDir.resolve("azure-entra.ini");
        String content = buildAzureEntraIniContent(tempDir);
        Files.write(iniFile, content.getBytes(StandardCharsets.UTF_8));

        // Execute WhiteRabbit with the INI and compare result with reference
        try {
            new WhiteRabbitMain(true, new String[]{"-ini", iniFile.toAbsolutePath().toString()});
        } catch (RuntimeException r) {
            if (r.getMessage().contains("is not currently available.  Please retry the connection later.")) {
                logger.warn("Please note that the exception thrown may be due to the database having been paused. Repeating this test a few seconds or minutes later may succeed.");
            }
            throw r;
        }

        URL referenceScanReport = SourceDataScanAzureEntraDefaultMethodIT.class.getClassLoader()
                .getResource("scan_data/ScanReport-reference-v0.10.7-sql.xlsx");
        assert referenceScanReport != null;
        assertTrue(ScanTestUtils.scanResultsSheetMatchesReference(
                tempDir.resolve("ScanReport.xlsx"),
                Paths.get(referenceScanReport.toURI()),
                DbType.AZURE_ENTRA));
    }

    private static String buildAzureEntraIniContent(Path workingFolder) {
        StringBuilder sb = new StringBuilder();
        // Generic scan settings
        sb.append("WORKING_FOLDER = ").append(workingFolder.toString()).append('\n');
        sb.append("DATA_TYPE = ").append(DbType.AZURE_ENTRA.label()).append('\n');
        sb.append("DELIMITER = ,\n");
        sb.append("TABLES_TO_SCAN = *\n");
        sb.append("SCAN_FIELD_VALUES = yes\n");
        sb.append("MIN_CELL_COUNT = 5\n");
        sb.append("MAX_DISTINCT_VALUES = 1000\n");
        sb.append("ROWS_PER_TABLE = 100000\n");
        sb.append("CALCULATE_NUMERIC_STATS = no\n");
        sb.append("NUMERIC_STATS_SAMPLER_SIZE = 500\n");

        // Azure Entra specific settings from properties file (loaded by PropertiesFileChecker)
        sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_SERVER).append(" = ")
                .append(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_SERVER")).append('\n');
        sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_DATABASE).append(" = ")
                .append(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_DATABASE")).append('\n');
        sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_SCHEMA).append(" = ")
                .append(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_SCHEMA")).append('\n');

        String authMethod = ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_AUTHENTICATION_METHOD");
        sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_AUTH_METHOD).append(" = ")
                .append(authMethod).append('\n');

//        // secrets are only added explicitly for the "client_secret" method
//        if ("client_secret".equalsIgnoreCase(authMethod)) {
//            sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_TENANT_ID).append(" = ")
//                    .append(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_TENANT_ID")).append('\n');
//            sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_CLIENT_ID).append(" = ")
//                    .append(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_CLIENT_ID")).append('\n');
//            sb.append(AzureSqlEntraHandler.AzureSqlConfiguration.AZURE_SQL_CLIENT_SECRET).append(" = ")
//                    .append(ScanTestUtils.getPropertyOrFail("ENTRA_WR_TEST_CLIENT_SECRET")).append('\n');
//        }

        return sb.toString();
    }
}
