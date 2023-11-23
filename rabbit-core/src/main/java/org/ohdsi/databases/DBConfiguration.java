package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;
import org.ohdsi.utilities.files.IniFile;

import java.io.PrintStream;
import java.util.*;

public abstract class DBConfiguration {
    public static final String ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD = "Multiple definitions for field ";
    private IniFile iniFile;
    protected ConfigurationFields configurationFields;

    private DBConfiguration() {
    }

    public DBConfiguration(IniFile inifile) {
        this.iniFile = inifile;
    }

    protected DBConfiguration(ConfigurationField... fields) {
        checkForDuplicates(fields);
        this.configurationFields = new ConfigurationFields(fields);
    }

    private void checkForDuplicates(ConfigurationField... fields) {
        Set<String> names = new HashSet<>();
        for (ConfigurationField field : fields) {
            if (names.contains(field.name)) {
                throw new DBConfigurationException(ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD + field.name);
            }
            names.add(field.name);
        }
    }

    public ValidationFeedback loadAndValidateConfiguration(IniFile iniFile) throws DBConfigurationException {
        for (ConfigurationField field : this.getFields()) {
            field.setValue(iniFile.get(field.name));
        }

        return this.validateAll();
    }

    public ValidationFeedback validateAll() {
        ValidationFeedback configurationFeedback = new ValidationFeedback();
        for (ConfigurationField field : this.getFields()) {
            for (FieldValidator validator : field.validators) {
                ValidationFeedback feedback = validator.validate(field);
                configurationFeedback.addWarnings(feedback.getWarnings());
                configurationFeedback.addErrors(feedback.getErrors());
            }
        }

        configurationFeedback.add(configurationFields.validate());

        return configurationFeedback;
    }

    public List<ConfigurationField> getFields() {
        return configurationFields.getFields();
    }

    public String getValue(String fieldName) {
        Optional<String> value = getFields().stream().filter(f -> fieldName.equalsIgnoreCase(f.name)).map(ConfigurationField::getValue).findFirst();
        return (value.orElse(""));
    }

    public void printIniFileTemplate(PrintStream stream) {
        for (ConfigurationField field : this.configurationFields.getFields()) {
            stream.printf("%s: %s\t%s%n",
                    field.name,
                    StringUtils.isEmpty(field.getDefaultValue()) ? "_" : field.getDefaultValue(),
                    field.toolTip);
        }
    }

    public interface FieldSet {
        List<ConfigurationField> getFields();

        default void generateIniFileFormat() {
            for (ConfigurationField field : getFields()) {
                System.out.println(String.format("%s:\t___\t# %s", field.name, field.toolTip));
            }
        }
    }

    public static class DBConfigurationException extends RuntimeException {
        public DBConfigurationException(String s) {
            super(s);
        }
    }

    ;

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

        public void add(ValidationFeedback feedback) {
            this.addWarnings(feedback.getWarnings());
            this.addErrors(feedback.getErrors());
        }
    }

    public class ConfigurationFields implements FieldSet {
        List<ConfigurationField> fields;
        List<ConfigurationValidator> validators = new ArrayList<>();

        public ConfigurationFields(ConfigurationField... fields) {
            this.fields = new ArrayList<>(Arrays.asList(fields));
        }

        public void addValidator(ConfigurationValidator validator) {
            this.validators.add(validator);
        }

        public List<ConfigurationField> getFields() {
            return this.fields;
        }

        public String getValue(String fieldName) {
            Optional<String> value = this.fields.stream().filter(f -> fieldName.equalsIgnoreCase(f.name)).map(ConfigurationField::getValue).findFirst();
            return (value.orElse(""));
        }

        public ValidationFeedback validate() {
            ValidationFeedback allFeedback = new ValidationFeedback();
            for (ConfigurationValidator validator : this.validators) {
                allFeedback.add(validator.validate(this));
            }

            return allFeedback;
        }
    }
}
