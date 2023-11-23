package org.ohdsi.databases;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationField {
    public final String name;
    public final String label;
    public final String toolTip;
    private String value;
    private String defaultValue;

    public static final String VALUE_REQUIRED_FORMAT_STRING = "A non-empty value is required for field %s (name %s)";
    List<DBConfiguration.FieldValidator> validators = new ArrayList<>();

    private static final DBConfiguration.FieldValidator fieldRequiredValidator = new FieldRequiredValidator();

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

    public ConfigurationField addValidator(DBConfiguration.FieldValidator validator) {
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

    private static class FieldRequiredValidator implements DBConfiguration.FieldValidator {
        public DBConfiguration.ValidationFeedback validate(ConfigurationField field) {
            DBConfiguration.ValidationFeedback feedback = new DBConfiguration.ValidationFeedback();
            if (StringUtils.isEmpty(field.getValue())) {
                feedback.addError(String.format(VALUE_REQUIRED_FORMAT_STRING, field.label, field.name));
            }

            return feedback;
        }
    }
}
