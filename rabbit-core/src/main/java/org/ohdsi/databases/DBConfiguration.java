package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;
import org.ohdsi.utilities.files.IniFile;

import java.io.PrintStream;
import java.util.*;

public abstract class DBConfiguration {
    public static final String ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD = "Multiple definitions for field ";
    private IniFile iniFile;
    private ConfigurationFields configurationFields;

    private DBConfiguration() {}
    public DBConfiguration(IniFile inifile) {
        this.iniFile = inifile;
    }

    protected DBConfiguration(ConfigurationField... fields) {
        checkForDuplicates(fields);
        this.configurationFields = new ConfigurationFields(fields);
    }

    private void checkForDuplicates(ConfigurationField... fields) {
        Set<String> names = new HashSet<>();
        for (ConfigurationField field: fields) {
            if (names.contains(field.name)) {
                throw new DBConfigurationException(ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD + field.name);
            }
            names.add(field.name);
        }
    }
    public ValidationFeedback validate() {
        ValidationFeedback configurationFeedback = new ValidationFeedback();
        for (ConfigurationField field: this.getFields()) {
            for (FieldValidator validator: field.validators) {
                ValidationFeedback feedback = validator.validate(field);
                configurationFeedback.addWarnings(feedback.getWarnings());
                configurationFeedback.addErrors(feedback.getErrors());
            }
        }

        return configurationFeedback;
    }

    public List<ConfigurationField> getFields() {
        return configurationFields.getFields();
    }
    public void printIniFileTemplate(PrintStream stream) {
        for (ConfigurationField field: this.configurationFields.getFields()) {
            stream.printf("%s: %s\t%s%n",
                    field.name,
                    StringUtils.isEmpty(field.getDefaultValue()) ? "_" : field.getDefaultValue(),
                    field.toolTip);
        }
    }
    public interface FieldSet {
        List<ConfigurationField> getFields();

        default void generateIniFileFormat() {
            for (ConfigurationField field: getFields()) {
                System.out.println(String.format("%s:\t___\t# %s", field.name, field.toolTip));
            }
        }
    }

    public static class DBConfigurationException extends RuntimeException {
        public DBConfigurationException(String s) {
            super(s);
        }
    };

    @FunctionalInterface
    public interface FieldValidator {
        ValidationFeedback validate(ConfigurationField field);
    }

    @FunctionalInterface
    public interface ConfigurationValidator {
        ValidationFeedback validate(ConfigurationFields fields);
    }

    public static class ValidationFeedback {
        private List<String> warnings = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public boolean isFullyValid() {
            return warnings.isEmpty() && errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getWarnings() {
            return this.warnings;
        }

        public List<String> getErrors() {
            return this.errors;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        public void addWarnings(List<String> warnings) {
            this.warnings.addAll(warnings);
        }
        public void addError(String error) {
            this.errors.add(error);
        }
        public void addErrors(List<String> errors) {
            this.errors.addAll(errors);
        }
    }

    public static class ConfigurationFields implements FieldSet {
        List<ConfigurationField> fields;

        public ConfigurationFields(ConfigurationField... fields) {
            this.fields = new ArrayList<>(Arrays.asList(fields));
        }

        public List<ConfigurationField> getFields() {
            return this.fields;
        }
    }

    public static class ConfigurationField {
        public final String name;
        public final String label;
        public final String toolTip;
        private String value;
        private String defaultValue;

        public static final String VALUE_REQUIRED_FORMAT_STRING = "A non-empty value is required for field %s (name %s)";
        private List<FieldValidator> validators = new ArrayList<>();

        private static FieldValidator fieldRequiredValidator = new FieldRequiredValidator();

        private ConfigurationField(String name, String label, String toolTip) {
            this.name = name;
            this.label = label;
            this.toolTip = toolTip;
            this.defaultValue = "";
        }

        public static ConfigurationField create(String name, String label, String toolTip) {
            return new ConfigurationField(name, label, toolTip);
        }

        public ConfigurationField required() {
            this.addValidator(fieldRequiredValidator);
            return this;
        }

        public ConfigurationField addDefaultValue(String value) {
            this.defaultValue = value;
            return this;
        }

        public ConfigurationField addValidator(FieldValidator validator) {
            this.validators.add(validator);
            return this;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        private static class FieldRequiredValidator implements FieldValidator {
            public ValidationFeedback validate(ConfigurationField field) {
                ValidationFeedback feedback = new ValidationFeedback();
                if (StringUtils.isEmpty(field.getValue())) {
                    feedback.addError(String.format(VALUE_REQUIRED_FORMAT_STRING, field.label, field.name));
                }

                return feedback;
            }
        }
    }
}
