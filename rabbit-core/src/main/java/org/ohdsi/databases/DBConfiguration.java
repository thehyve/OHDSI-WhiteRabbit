package org.ohdsi.databases;

import one.util.streamex.EntryStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.ohdsi.utilities.files.IniFile;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DBConfiguration {
    public static final String DELIMITER_FIELD = "DELIMITER";
    public static final String TABLES_TO_SCAN_FIELD = "TABLES_TO_SCAN";
    public static final String SCAN_FIELD_VALUES_FIELD = "SCAN_FIELD_VALUES";
    public static final String MIN_CELL_COUNT_FIELD = "MIN_CELL_COUNT";
    public static final String MAX_DISTINCT_VALUES_FIELD = "MAX_DISTINCT_VALUES";
    public static final String ROWS_PER_TABLE_FIELD = "ROWS_PER_TABLE";
    public static final String CALCULATE_NUMERIC_STATS_FIELD = "CALCULATE_NUMERIC_STATS";
    public static final String NUMERIC_STATS_SAMPLER_SIZE_FIELD = "NUMERIC_STATS_SAMPLER_SIZE";
    public static final String ERROR_DUPLICATE_DEFINITIONS_FOR_FIELD = "Multiple definitions for field ";
    private IniFile iniFile;
    protected ConfigurationFields configurationFields;

    private DBConfiguration() {
    }

    public DBConfiguration(IniFile inifile) {
        this.iniFile = inifile;
    }

    protected DBConfiguration(ConfigurationField... fields) {
        new DBConfiguration(true, fields);
    }

    protected DBConfiguration(boolean withDefaults, ConfigurationField... fields) {
        if (withDefaults) {
            fields = (ConfigurationField[]) ArrayUtils.addAll(fields, defaultConfigurationFields());
        }
        checkForDuplicates(fields);
        this.configurationFields = new ConfigurationFields(fields);
    }

    private ConfigurationField[] defaultConfigurationFields() {
        return new ConfigurationField[]{
                ConfigurationField.create(DELIMITER_FIELD,
                                "",
                                "")
                        .defaultValue(",")
                        .required(),
                ConfigurationField.create(TABLES_TO_SCAN_FIELD,
                                "",
                                "")
                        .defaultValue("*")
                        .required(),
                ConfigurationField.create(SCAN_FIELD_VALUES_FIELD,
                                "",
                                "")
                        .defaultValue("yes")
                        .required(),
                ConfigurationField.create(MIN_CELL_COUNT_FIELD,
                                "",
                                "")
                        .defaultValue("5")
                        .integerValue()
                        .required(),
                ConfigurationField.create(MAX_DISTINCT_VALUES_FIELD,
                                "",
                                "")
                        .defaultValue("1000")
                        .integerValue()
                        .required(),
                ConfigurationField.create(ROWS_PER_TABLE_FIELD,
                                "",
                                "")
                        .defaultValue("100000")
                        .integerValue()
                        .required(),
                ConfigurationField.create(CALCULATE_NUMERIC_STATS_FIELD,
                                "",
                                "")
                        .defaultValue("no")
                        .yesNoValue()
                        .required(),
                ConfigurationField.create(NUMERIC_STATS_SAMPLER_SIZE_FIELD,
                        "",
                        "")
                        .defaultValue("500")
                        .integerValue()
                        .required()
        };
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
                configurationFeedback.add(feedback);
            }
        }

        configurationFeedback.add(configurationFields.validate());

        return configurationFeedback;
    }

    public List<ConfigurationField> getFields() {
        return configurationFields.getFields();
    }

    public ConfigurationField getField(String fieldName) {
        return this.getFields().stream().filter(f -> f.name.equalsIgnoreCase(fieldName)).findFirst().orElse(null);
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
        private Map<String, List<ConfigurationField>> warnings = new HashMap<>();
        private Map<String, List<ConfigurationField>> errors = new HashMap<>();

        public boolean isFullyValid() {
            return warnings.isEmpty() && errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public Map<String, List<ConfigurationField>> getWarnings() {
            return this.warnings;
        }

        public Map<String, List<ConfigurationField>> getErrors() {
            return this.errors;
        }

        public void addWarning(String warning, ConfigurationField field) {
            if (this.warnings.containsKey(warning)) {
                this.warnings.get(warning).add(field);
            } else {
                this.warnings.put(warning, Collections.singletonList(field));
            }
        }

        public void addError(String error, ConfigurationField field) {
            if (this.errors.containsKey(error)) {
                this.errors.get(error).add(field);
            } else {
                this.errors.put(error, Stream.of(field).collect(Collectors.toList()));
            }
        }

        public void add(ValidationFeedback feedback) {
            this.warnings = EntryStream.of(this.warnings)
                    .append(EntryStream.of(feedback.getWarnings()))
                    .toMap((e1, e2) -> e1);
            this.errors = EntryStream.of(this.errors)
                    .append(EntryStream.of(feedback.getErrors()))
                    .toMap((e1, e2) -> e1);
        }
    }

    public static class ConfigurationFields {
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

        public ConfigurationField get(String fieldName) {
            Optional<ConfigurationField> field = fields.stream().filter(f -> fieldName.equalsIgnoreCase(f.name)).findFirst();
            if (field.isPresent()) {
                return field.get();
            }

            throw new DBConfigurationException(String.format("No ConfigurationField object found for field name '%s'", fieldName));
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
