package org.ohdsi.whiteRabbit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.ohdsi.databases.*;
import org.ohdsi.utilities.files.IniFile;
import org.ohdsi.utilities.files.Row;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.whiteRabbit.scan.SourceDataScan;
import org.ohdsi.whiteRabbit.scan.UniformSamplingReservoir;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({SourceDataScan.class, WhiteRabbitMain.class, RichConnection.class, TestWhiteRabbitMain.class})
public class TestWhiteRabbitMain extends RichConnection /* so that we can extend QueryResult */ {

    public static final String DATA_TYPE_FIELD = "DATA_TYPE";
    public static final String TABLES_TO_SCAN_FIELD = "TABLES_TO_SCAN";
    public static final String SCAN_FIELD_VALUES_FIELD = "SCAN_FIELD_VALUES";
    public static final String MIN_CELL_COUNT_FIELD = "MIN_CELL_COUNT";
    public static final String MAX_DISTINCT_VALUES_FIELD = "MAX_DISTINCT_VALUES";
    public static final String ROWS_PER_TABLE_FIELD = "ROWS_PER_TABLE";
    public static final String CALCULATE_NUMERIC_STATS_FIELD = "CALCULATE_NUMERIC_STATS";
    public static final String NUMERIC_STATS_SAMPLER_SIZE_FIELD = "NUMERIC_STATS_SAMPLER_SIZE";
    public static final long PROGRESS_INTERVAL = 10_000_000;
    public static final long XSAMPLES = 100_000_000;
    public static final long BOUND = Integer.MAX_VALUE;
    public static final long RANDOM_SEED = 556365;
    public static final boolean CALC_STATS = true;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testPerformSourceDataScan() throws Exception {
        Path tempDir = temporaryFolder.getRoot().toPath();

        setupMockedRichConnection();
        setupSpiedSourceDataScan(XSAMPLES, BOUND);

        UniformSamplingReservoir.RandomUtil.setSeed(RANDOM_SEED);

        Path scanReportPath = Paths.get(tempDir.toFile().getAbsolutePath(), "ScanReport.xlsx");
        Path iniFilePath = Paths.get(tempDir.toFile().getAbsolutePath(), "LargeScan.ini");
        IniFile iniFile = createIniFileForTest(tempDir);
        writeIniFile(iniFile, iniFilePath);
        String reportFilePath = WhiteRabbitMain.launchCommandLine(iniFilePath.toString());
        assertNotNull(reportFilePath);
        Path testScanResult = Paths.get(
                String.format("/var/tmp/ScanReport-v0.10.7-%s-%s%s%s.xlsx",
                        XSAMPLES, BOUND, CALC_STATS ? "-stats": "", RANDOM_SEED == 0 ? "" : "-seed-" + RANDOM_SEED));
        Files.copy(scanReportPath, testScanResult, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Result copied to: " + testScanResult);

        //assertTrue(ScanTestUtils.scanResultsSheetMatchesReference(tempDir.resolve("ScanReport.xlsx"), Paths.get(referenceScanReport.toURI()), DbType.DELIMITED_TEXT_FILES));
    }

    private void setupSpiedSourceDataScan(long xSamples, long bound) throws Exception {
        SourceDataScan spiedSourceDataScan = spy(new SourceDataScan());
        whenNew(SourceDataScan.class).withAnyArguments().thenReturn(spiedSourceDataScan);

        doAnswer((Answer<List<SourceDataScan.FieldInfo>>) invocation -> {
            String tableName = invocation.getArgument(1).toString();
            return generateTableStructure(tableName);
        }).when(spiedSourceDataScan).fetchTableStructure(any(RichConnection.class), anyString());

        doAnswer((Answer<RichConnection.QueryResult>) invocation -> {
            String tableName = invocation.getArgument(2).toString();
            return new GeneratingQueryResult(xSamples, bound);
        }).when(spiedSourceDataScan).fetchRowsFromTable(any(RichConnection.class), anyString(), anyLong());
    }

    private void setupMockedRichConnection() throws Exception {
        RichConnection mockedRichConnection = mock(RichConnection.class);
        whenNew(RichConnection.class).withAnyArguments().thenReturn(mockedRichConnection);
        when(mockedRichConnection.getTableNames(anyString())).thenReturn(Arrays.asList("LARGE"));
        doAnswer((Answer<Long>) invocation -> {
            String tableName = invocation.getArgument(0).toString();
            if (tableName.equals("LARGE")) {
                return BOUND;
            }
            throw new RuntimeException("Unknown table: " + tableName);
        }).when(mockedRichConnection).getTableSize(anyString());
    }

    public class GeneratingQueryResult extends RichConnection.QueryResult {

        private long xSample;
        private long bound;
        public GeneratingQueryResult(long xSample, long bound) {
            super(null);
            this.xSample = xSample;
            this.bound = bound;
        }

        public Iterator<Row> iterator() {
            return new GeneratingIterator(xSample, bound);
        }

        public void close() {
        }
    }

    public class GeneratingIterator implements Iterator<Row> {

        private SampleSequence sequence;

        public GeneratingIterator(long xSample, long bound) {
            this.sequence = new SampleSequence(xSample, bound);
        }
        @Override
        public boolean hasNext() {
            return this.sequence.hasNext();
        }

        @Override
        public Row next() {
            long val = sequence.next();
            Map<String, Integer> field2column = new HashMap<>();
            field2column.put("table_id", 0);
            field2column.put("X", 1);
            field2column.put("C", 2);
            field2column.put("M", 3);
            field2column.put("MM", 4);
            field2column.put("MMM", 5);
            field2column.put("Parity", 6);
            List<String> cells = Arrays.asList(
                    String.valueOf(val),
                    String.valueOf(val % 10),
                    String.valueOf(val % 100),
                    String.valueOf(val % 1_000),
                    String.valueOf(val % 1_000_000),
                    String.valueOf(val % 1_000_000_000),
                    val % 2 == 0 ? "even" : "odd"
            );

            return new Row(cells, field2column);
        }
    }

    private List<SourceDataScan.FieldInfo> generateTableStructure(String tableName) {
        switch (tableName) {
            case "LARGE": return generateLargeTableStructure();
            default: throw new RuntimeException("Unknown table: " + tableName);
        }
    }

    private List<SourceDataScan.FieldInfo> generateLargeTableStructure() {
        List<SourceDataScan.FieldInfo> fieldInfos = new ArrayList<>();
        fieldInfos.add(new SourceDataScan.FieldInfo("table_id" ));
        fieldInfos.add(new SourceDataScan.FieldInfo("X"));
        fieldInfos.add(new SourceDataScan.FieldInfo("C"));
        fieldInfos.add(new SourceDataScan.FieldInfo("M"));
        fieldInfos.add(new SourceDataScan.FieldInfo("MM"));
        fieldInfos.add(new SourceDataScan.FieldInfo("MMM"));
        fieldInfos.add(new SourceDataScan.FieldInfo("Parity"));
        return fieldInfos;
    }

    private static IniFile createIniFileForTest(Path tempDir) {
        IniFile iniFile = new IniFile();

        iniFile.set(DATA_TYPE_FIELD, DbType.POSTGRESQL.getTypeName());
        iniFile.set("USER_NAME", "joe");
        iniFile.set("WORKING_FOLDER", tempDir.toString());
        iniFile.set("SERVER_LOCATION", "123.123.123.123/no_database");
        iniFile.set("PASSWORD", "mycatsname");
        iniFile.set("DATABASE_NAME", "schema_name");
        iniFile.set(TABLES_TO_SCAN_FIELD, "*");
        iniFile.set(SCAN_FIELD_VALUES_FIELD, "yes");
        iniFile.set(MIN_CELL_COUNT_FIELD, "5");
        iniFile.set(MAX_DISTINCT_VALUES_FIELD, "1000");
        iniFile.set(ROWS_PER_TABLE_FIELD, "10000000");
        iniFile.set(CALCULATE_NUMERIC_STATS_FIELD, CALC_STATS ? "yes" : "no");
        iniFile.set(NUMERIC_STATS_SAMPLER_SIZE_FIELD, "500");

        return iniFile;
    }

    private static SourceDataScan scanParameters = new SourceDataScan();
    private SourceDataScan getScanParameters() {
        return scanParameters;
    }

    //@Test
    public void testSampleSequence() {
        for (int x = 1; x < 100; ++x) {
            for (int max = 100; max < 1000; ++max) {
                SampleSequence seq = new SampleSequence(x, max);
                long val = seq.next();
                while (val != 0) {
                    val = seq.next();
                }
                assertEquals(x, seq.getGenerated());
            }
        }

        SampleSequence seq = new SampleSequence(100, 50);
        long val = seq.next();
        while (val != 0) {
            //System.out.println("next: " + val);
            val = seq.next();
        }
        assertEquals(50, seq.getGenerated());
    }

    private static class SampleSequence {
        private long current = 0;
        private long increment = 1;
        private long generated = 0;
        private final long bound;
        private final long xSamples;
        public SampleSequence(long xSamples, long bound) {
            increment = max(1, bound / xSamples);
            this.bound = bound;
            this.xSamples = xSamples;
        }

        long next() {
            if (current >= bound || generated >= xSamples) {
                return 0;
            } else if (bound - increment < current && generated < bound) {
                current = bound;
                if (++generated < xSamples) {
                    throw new RuntimeException("Did not generate enough values");
                }
                return current;
            }

            current += increment;
            ++generated;
            if (generated % PROGRESS_INTERVAL == 0) {
                System.out.print(".");
            }
            return current;
        }

        boolean hasNext() {
            return (current < bound && generated < xSamples);
        }

        public long getGenerated() {
            return generated;
        }
    }

    private void writeIniFile(IniFile inifile, Path iniFilePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(
                iniFilePath))) {
            inifile.getSettings().forEach((key, value) -> pw.printf("%s = %s%n", key.toUpperCase(), value));
        }
    }
}