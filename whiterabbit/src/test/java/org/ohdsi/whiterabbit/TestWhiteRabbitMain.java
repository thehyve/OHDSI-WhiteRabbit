package org.ohdsi.whiterabbit;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ohdsi.databases.*;
import org.ohdsi.databases.configuration.DBConfiguration;
import org.ohdsi.databases.configuration.DbSettings;
import org.ohdsi.databases.configuration.DbType;
import org.ohdsi.utilities.files.IniFile;
import org.ohdsi.utilities.files.Row;
import org.ohdsi.whiterabbit.scan.ScanTestUtils;
import org.ohdsi.whiterabbit.scan.SourceDataScan;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.nio.file.*;
import java.util.*;

import static java.lang.Math.max;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({SourceDataScan.class, WhiteRabbitMain.class, RichConnection.class})
public class TestWhiteRabbitMain {

    private static final long XSAMPLES = 100_000_000;
    private static final long BOUND = Integer.MAX_VALUE;
    private static final long RANDOM_SEED = 556365;

    private static final boolean CALC_STATS = true;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /*
     * we need ScanParameters to generate FieldInfo objects, but its values are actually set by
     * WhiteRabbitMain.performSourceDataScan(), so we create a static that we pass to WhiteRabbitMain.performSourceDataScan(),
     * so that it gets set appropriately and is then available when FieldInfo objects are being generated
     */
    private static final SourceDataScan scanParameters = new SourceDataScan();

    @Test
    public void testPerformSourceDataScan() throws Exception {
        Path tempDir = temporaryFolder.getRoot().toPath();

        UniformSamplingReservoir.RandomUtil.setSeed(RANDOM_SEED);

        createMockedRichConnection(XSAMPLES, BOUND);

        URL referenceScanReport = TestWhiteRabbitMain.class.getClassLoader().getResource(
                String.format("scan_data/ScanReport-v0.10.7-reference-%s-%s-stats-seed-%s.xlsx", XSAMPLES, BOUND, RANDOM_SEED));
        assertTrue(Files.exists(Paths.get(referenceScanReport.toURI())), "Reference file does not exist: " + referenceScanReport);

        IniFile iniFile = createIniFileForTest(tempDir);
        DbSettings dbSettings = WhiteRabbitMain.getDbSettings(iniFile);
        WhiteRabbitMain.findTablesToScan(iniFile, dbSettings);
        String reportFilePath = WhiteRabbitMain.performSourceDataScan(createIniFileForTest(tempDir), dbSettings, Optional.of(scanParameters));
        assertNotNull(reportFilePath);
        Path testScanResult = Paths.get(String.format("/var/tmp/ScanResult-v1.0.0-%s-%s%s%s.xlsx",
                XSAMPLES, BOUND, CALC_STATS ? "-stats" : "", RANDOM_SEED == 0 ? "" : "-seed-" + RANDOM_SEED));
        Files.copy(Paths.get(tempDir.toFile().getAbsolutePath(), "ScanReport.xlsx"), testScanResult, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Result copied to: " + testScanResult);

        List<String> differences = ScanTestUtils.getNonMatchingValues(ScanTestUtils.readXlsxAsStringValues(Paths.get(reportFilePath)),
                ScanTestUtils.readXlsxAsStringValues(Paths.get(referenceScanReport.toURI())), "Field Overview", "Table Overview", "LARGE", "_");
        assertEquals(3, differences.size());
        // expected differences are 2 timestamps (start/finish time of scan) and the version. Filtering these out should result in a difference count of 0.
        assertEquals(0, differences.stream().filter(s -> !s.contains("2023-12-") && !s.contains("0.10.7")).count());
    }

    private void createMockedRichConnection(long xSamples, long bound) throws Exception {
        RichConnection mockedRichConnection = mock(RichConnection.class);
        whenNew(RichConnection.class).withAnyArguments().thenReturn(mockedRichConnection);
        when(mockedRichConnection.getTableNames(anyString())).thenReturn(Collections.singletonList("LARGE"));
        when(mockedRichConnection.supportsDBConnectionInterface()).thenReturn(false);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) {
                String tableName = invocation.getArgument(0).toString();
                if (tableName.equals("LARGE")) {
                    return bound;
                }
                throw new RuntimeException("Unknown table: " + tableName);
            }
        }).when(mockedRichConnection).getTableSize(anyString());

        doAnswer((Answer<List<FieldInfo>>) invocation -> {
            String tableName = invocation.getArgument(2).toString();
            return generateTableStructure(tableName);
        }).when(mockedRichConnection).fetchTableStructure(any(RichConnection.class), anyString(), anyString(), any(SourceDataScan.class));

        doAnswer((Answer<QueryResult>) invocation -> {
            String tableName = invocation.getArgument(2).toString();
            return new GeneratingQueryResult(xSamples, bound);
        }).when(mockedRichConnection).fetchRowsFromTable(anyString(), anyLong(), any(SourceDataScan.class));
    }

    private class GeneratingQueryResult extends QueryResult {
        private final long xSample;
        private final long bound;
        public GeneratingQueryResult(long xSample, long bound) {
            super("", null);
            this.xSample = xSample;
            this.bound = bound;
        }

        @NotNull
        public Iterator<Row> iterator() {
            return new GeneratingIterator(xSample, bound);
        }

        public void close() {
        }
    }

    public static class GeneratingIterator implements Iterator<Row> {

        private final SampleSequence sequence;

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

    private List<FieldInfo> generateTableStructure(String tableName) {
        if (tableName.equals("LARGE")) {
            return generateLargeTableStructure();
        }
        throw new RuntimeException("Unknown table: " + tableName);
    }

    private List<FieldInfo> generateLargeTableStructure() {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        fieldInfos.add(new FieldInfo(scanParameters, "table_id" ));
        fieldInfos.add(new FieldInfo(scanParameters, "X"));
        fieldInfos.add(new FieldInfo(scanParameters, "C"));
        fieldInfos.add(new FieldInfo(scanParameters, "M"));
        fieldInfos.add(new FieldInfo(scanParameters, "MM"));
        fieldInfos.add(new FieldInfo(scanParameters, "MMM"));
        fieldInfos.add(new FieldInfo(scanParameters, "Parity"));
        return fieldInfos;
    }

    private static IniFile createIniFileForTest(Path tempDir) {
        IniFile iniFile = new IniFile();

        iniFile.set(DBConfiguration.DATA_TYPE_FIELD, DbType.POSTGRESQL.label());
        iniFile.set("USER_NAME", "joe");
        iniFile.set("WORKING_FOLDER", tempDir.toString());
        iniFile.set("SERVER_LOCATION", "123.123.123.123/no_database");
        iniFile.set("PASSWORD", "mycatsname");
        iniFile.set("DATABASE_NAME", "schema_name");
        iniFile.set(DBConfiguration.TABLES_TO_SCAN_FIELD, "*");
        iniFile.set(DBConfiguration.SCAN_FIELD_VALUES_FIELD, "yes");
        iniFile.set(DBConfiguration.MIN_CELL_COUNT_FIELD, "5");
        iniFile.set(DBConfiguration.MAX_DISTINCT_VALUES_FIELD, "1000");
        iniFile.set(DBConfiguration.ROWS_PER_TABLE_FIELD, "10000000");
        iniFile.set(DBConfiguration.CALCULATE_NUMERIC_STATS_FIELD, CALC_STATS ? "yes" : "no");
        iniFile.set(DBConfiguration.NUMERIC_STATS_SAMPLER_SIZE_FIELD, "500");

        return iniFile;
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
            if (generated % 10_000_000 == 0) {
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
}