package org.ohdsi.rabbitInAHat.dataModel;

import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TestDatabase {

    @Test
    void testGenerateModelFromCSV() throws IOException {
        // confirm that issue #411 is fixed, can read (UTF-8) files with and without BOM
        String testFileWithBom = "tiny_riah_with_bom.csv";
        String testFileWithoutBom = "tiny_riah_without_bom.csv";
        InputStream inWithBom = TestDatabase.class.getResourceAsStream(testFileWithBom);
        InputStream inWithoutBom = TestDatabase.class.getResourceAsStream(testFileWithoutBom);
        assertNotNull(inWithBom);
        assertNotNull(inWithoutBom);
        Database ignoredWithBom = Database.generateModelFromCSV(inWithBom, testFileWithBom);
        Database ignoredWithoutBom = Database.generateModelFromCSV(inWithoutBom, testFileWithoutBom);
    }
}