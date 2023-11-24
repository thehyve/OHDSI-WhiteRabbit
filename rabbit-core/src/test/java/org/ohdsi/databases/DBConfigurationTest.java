package org.ohdsi.databases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.ohdsi.databases.ConfigurationField.VALUE_REQUIRED_FORMAT_STRING;
import static org.ohdsi.databases.DBConfiguration.ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD;

class DBConfigurationTest {

    private final String NAME_FIELD1 = "FIELD_1";
    private final String LABEL_FIELD1 = "Field one";
    private final String TOOLTIP_FIELD1 = "Tooltip for field one";
    private final String NAME_FIELD2 = "FIELD_2";
    private final String LABEL_FIELD2 = "Field two";
    private final String TOOLTIP_FIELD2 = "Tooltip for field two";

    @BeforeEach
    void setUp() {
    }

    @Test
    void doNotAcceptDuplicateDefinitionsForField() {
    Exception exception = assertThrows(DBConfiguration.DBConfigurationException.class, () -> {
        DBConfiguration testConfiguration = new DBConfiguration(
                ConfigurationField.create(NAME_FIELD1, LABEL_FIELD1, TOOLTIP_FIELD1).required(),
                ConfigurationField.create(NAME_FIELD1, LABEL_FIELD2, TOOLTIP_FIELD2));
        });
        assertTrue(exception.getMessage().startsWith(ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD));
    }

    @Test
    void getFields() {
    }

    @Test
    void printIniFileTemplate() {
    }
}