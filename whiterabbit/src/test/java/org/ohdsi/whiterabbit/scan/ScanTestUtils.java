package org.ohdsi.whiterabbit.scan;

import org.apache.commons.lang.StringUtils;
import org.ohdsi.databases.DbType;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.ooxml.ReadXlsxFileWithHeader;
import org.ohdsi.utilities.files.Row;
import org.ohdsi.utilities.files.RowUtilities;
import org.ohdsi.databases.DbSettings;
import org.ohdsi.whiteRabbit.scan.SourceDataScan;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScanTestUtils {

    // Convenience for having the same scan parameters across tests
    public static SourceDataScan createSourceDataScan() {
        SourceDataScan sourceDataScan = new SourceDataScan();
        sourceDataScan.setMinCellCount(5);
        sourceDataScan.setScanValues(true);
        sourceDataScan.setMaxValues(1000);
        sourceDataScan.setNumStatsSamplerSize(500);
        sourceDataScan.setCalculateNumericStats(false);
        sourceDataScan.setSampleSize(100000);

        return sourceDataScan;
    }

    public static void verifyScanResultsFromXSLX(Path results, DbType dbType) {
        assertTrue(Files.exists(results));

        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(results.toUri()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("File %s was expected to be found, but does not exist.", results), e);
        }

        ReadXlsxFileWithHeader sheet = new ReadXlsxFileWithHeader(file);

        List<Row> data = new ArrayList<>();
        int i = 0;
        for (Row row : sheet) {
            data.add(row);
            i++;
        }

        // apparently the order of rows in the generated xslx table is not fixed,
        // so they need to be sorted to be able to verify their contents
        RowUtilities.sort(data, "Table", "Field");
        assertEquals(42, i);

        // since the table is generated with empty lines between the different tables of the source database,
        // a number of empty lines is expected. Verify this, and the first non-empty line
        expectRowNIsLike(0, data, dbType, "", "", "", "", "", "");
        expectRowNIsLike(1, data, dbType, "", "", "", "", "", "");
        expectRowNIsLike(2, data, dbType, "cost", "amount_allowed", "", "numeric", "0", "34");

        // sample some other rows in the available range
        expectRowNIsLike(9, data,dbType, "cost", "drg_source_value", "", "character varying", "0", "34");
        expectRowNIsLike(23, data,dbType, "cost", "total_paid", "", "numeric", "3", "34");
        expectRowNIsLike(24, data,dbType, "person", "birth_datetime", "", "timestamp without time zone", "0", "30");
        expectRowNIsLike(41, data,dbType, "person", "year_of_birth", "", "integer", "4", "30");
    }

    public static void compareScanResultsToReference(Path scanResults, Path referenceResults) {
        List<Row> scanRows = readXlsxAsSortedRows(scanResults);
        List<Row> referenceRows = readXlsxAsSortedRows(referenceResults);

        assertEquals(scanRows.size(), referenceRows.size());
        for (int i = 0; i < scanRows.size(); ++i) {
            assertEquals(0, (new RowComparator()).compare(scanRows.get(i), referenceRows.get(i)));
        }
    }
    private static void expectRowNIsLike(int n, List<Row> rows, DbType dbType, String... expectedValues) {
        assert expectedValues.length == 6;
        if (!dbType.equals(DbType.DELIMITED_TEXT_FILES)) {
            testColumnValue(n, rows.get(n), "Table", expectedValues[0]);
            testColumnValue(n, rows.get(n), "Field", expectedValues[1]);
            testColumnValue(n, rows.get(n), "Description", expectedValues[2]);
            testColumnValue(n, rows.get(n), "Type", expectedTypeValue(expectedValues[3], dbType));
            testColumnValue(n, rows.get(n), "Max length", expectedValues[4]);
            testColumnValue(n, rows.get(n), "N rows", expectedValues[5]);
        }
    }

    private static void testColumnValue(int i, Row row, String fieldName, String expected) {
        if (!expected.equalsIgnoreCase(row.get(fieldName))) {
            if (!row.get(fieldName).equals("EMPTY")) {  // in case of csv, EMPTY can show up, this cannot be matched to a single type
                fail(String.format("In row %d, value '%s' was expected for column '%s', but '%s' was found",
                        i, expected, fieldName, row.get(fieldName)));
            }
        }
    }

    private static String expectedTypeValue(String postgresType, DbType dbType) {
        /*
         * This is very pragmatical and may need to change when tests are added for more databases.
         * For now, PostgreSQL is used as the reference, and the expected types need to be adapted to match
         * for other database.
         *
         * IN: expected type for postgres; OUT: expected type for database type dbType
         */
        if (dbType == DbType.POSTGRESQL || postgresType.equals("")) {
            return postgresType;
        }
        else if (dbType == DbType.ORACLE) {
            switch (postgresType) {
                case "integer":
                    return "NUMBER";
                case "numeric":
                    return "FLOAT";
                case "character varying":
                    return "VARCHAR2";
                case "timestamp without time zone":
                    // seems a mismatch in the OMOP CMD v5.2 (Oracle defaults to WITH time zone)
                    return "TIMESTAMP(6) WITH TIME ZONE";
                default:
                    throw new RuntimeException("Unsupported column type: " + postgresType);
            }
        }
        else if (dbType == DbType.SNOWFLAKE) {
            switch (postgresType) {
                case "integer":
                case "numeric":
                    return "NUMBER";
                case "character varying":
                    return "VARCHAR";
                case "timestamp without time zone":
                    return "TIMESTAMPNTZ";
                default:
                    throw new RuntimeException("Unsupported column type: " + postgresType);
            }
        }
        else if (dbType == DbType.DELIMITED_TEXT_FILES) {
            switch (postgresType) {
                case "integer":
                    return "NUMBER";
                case "numeric":
                    return "FLOAT";
                case "character varying":
                    return "VARCHAR2";
                case "timestamp without time zone":
                    // seems a mismatch in the OMOP CMD v5.2 (Oracle defaults to WITH time zone)
                    return "TIMESTAMP(6) WITH TIME ZONE";
                default:
                    throw new RuntimeException("Unsupported column type: " + postgresType);
            }
        }
        else {
            throw new RuntimeException("Unsupported DbType: " + dbType.getTypeName());
        }
    }

    static DbSettings getTestPostgreSQLSettings(PostgreSQLContainer<?> container) {
        DbSettings dbSettings = new DbSettings();
        dbSettings.dbType = DbType.POSTGRESQL;
        dbSettings.sourceType = DbSettings.SourceType.DATABASE;
        dbSettings.server = container.getJdbcUrl();
        dbSettings.database = "public"; // yes, really
        dbSettings.user = container.getUsername();
        dbSettings.password = container.getPassword();
        dbSettings.tables = getTableNamesPostgreSQL(dbSettings);

        return dbSettings;
    }

    static List<String> getTableNamesPostgreSQL(DbSettings dbSettings) {
        try (RichConnection richConnection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType)) {
            return richConnection.getTableNames("public");
        }
    }

    private static List<Row> readXlsxAsSortedRows(Path xlsx) {
        assertTrue(Files.exists(xlsx));

        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(xlsx.toUri()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("File %s was expected to be found, but does not exist.", xlsx), e);
        }

        ReadXlsxFileWithHeader sheet = new ReadXlsxFileWithHeader(file);

        List<Row> rows = new ArrayList<>();
        for (Row row : sheet) {
            rows.add(row);
        }

        // apparently the order of rows in the generated xslx table is not fixed,
        // so they need to be sorted to be able to verify their contents
        RowUtilities.sort(rows, "Table", "Field");

        return rows;
    }

    private static class RowComparator implements Comparator<Row> {
        @Override
        public int compare(Row row1, Row row2) {
            List<String> fieldNames1 = row1.getFieldNames();
            List<String> fieldNames2 = row2.getFieldNames();

            assertIterableEquals(fieldNames1, fieldNames2, "Field names of rows do not match.");
            for (String name: fieldNames1) {
                assertEquals(row1.get(name), row2.get(name), "Values of fields do not match.");
            }
            return 0;
        }
    }
}
