package org.ohdsi.databases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.ohdsi.databases.DBConfiguration.ConfigurationField.VALUE_REQUIRED_FORMAT_STRING;
import static org.ohdsi.databases.DBConfiguration.ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD;

class DBConfigurationTest {

    private final String NAME_FIELD1 = "FIELD_1";
    private final String LABEL_FIELD1 = "Field one";
    private final String TOOLTIP_FIELD1 = "Tooltip for field one";
    private final String NAME_FIELD2 = "FIELD_2";
    private final String LABEL_FIELD2 = "Field two";
    private final String TOOLTIP_FIELD2 = "Tooltip for field two";
    private class TestConfiguration extends DBConfiguration {
        public TestConfiguration(ConfigurationField... fields) {
            super(fields);
        }
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    void validateRequiredField() {
        TestConfiguration testConfiguration = new TestConfiguration(
                DBConfiguration.ConfigurationField.create(NAME_FIELD1, LABEL_FIELD1, TOOLTIP_FIELD1).required(),
                DBConfiguration.ConfigurationField.create(NAME_FIELD2, LABEL_FIELD2, TOOLTIP_FIELD2));

        DBConfiguration.ValidationFeedback feedback = testConfiguration.validate();
        assertEquals(0, feedback.getWarnings().size(), "There should be no warnings");
        assertEquals(1, feedback.getErrors().size(), "There should be 1 error");
        assertEquals(feedback.getErrors().get(0), String.format(VALUE_REQUIRED_FORMAT_STRING, LABEL_FIELD1, NAME_FIELD1),
                String.format("Error should indicate that field %s has no value", NAME_FIELD1));
    }

    @Test
    void doNotAcceptDuplicateDefinitionsForField() {
    Exception exception = assertThrows(DBConfiguration.DBConfigurationException.class, () -> {
        TestConfiguration testConfiguration = new TestConfiguration(
                DBConfiguration.ConfigurationField.create(NAME_FIELD1, LABEL_FIELD1, TOOLTIP_FIELD1).required(),
                DBConfiguration.ConfigurationField.create(NAME_FIELD1, LABEL_FIELD2, TOOLTIP_FIELD2));
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