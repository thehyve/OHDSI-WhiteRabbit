package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.ohdsi.databases.configuration.ConfigurationField;
import org.ohdsi.databases.configuration.ScanConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAzureSqlEntraHandler {

    @Test
    void testPrintIniFileTemplate() throws IOException {
        String output;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); PrintStream printStream = new PrintStream(outputStream)) {
            ScanConfiguration configuration = new AzureSqlEntraHandler.AzureSqlConfiguration();
            configuration.printIniFileTemplate(printStream);
            output = outputStream.toString();
            for (ConfigurationField field: configuration.getFields()) {
                assertTrue(output.contains(field.name), String.format("ini file template should contain field name (%s)", field.name));
                assertTrue(output.contains(field.toolTip), String.format("ini file template should contain tool tip (%s)", field.toolTip));
                if (!StringUtils.isEmpty(field.getDefaultValue())) {
                    assertTrue(output.contains(field.getDefaultValue()), String.format("ini file template should contain default value (%s)", field.getDefaultValue()));
                }
            }
        }
    }

    //@Test
    void testSampleQueryBuilds() {
        AzureSqlEntraHandler handler = AzureSqlEntraHandler.INSTANCE;
        String sql = handler.getRowSampleQuery("mytable", 1000, 100);
        assertTrue(sql.contains("SELECT TOP 100"));
        assertTrue(sql.contains("[dbo].[mytable]"));
        assertTrue(sql.toUpperCase().contains("ORDER BY NEWID()"));
    }
}
