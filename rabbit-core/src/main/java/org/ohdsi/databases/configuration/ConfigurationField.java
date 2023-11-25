package org.ohdsi.databases.configuration;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigurationField {
    public final String name;
    public final String label;
    public final String toolTip;
    private String value;
    private String defaultValue;

    public static final String VALUE_REQUIRED_FORMAT_STRING = "A non-empty value is required for field %s (name %s)";
    public static final String INTEGER_VALUE_REQUIRED_FORMAT_STRING = "An integer value is allowed for field %s (name %s)";
    public static final String ONLY_YESNO_ALLOWED_FORMAT_STRING = "Only the values 'yes' or 'no' are allowed for field %s (name %s)";

    List<FieldValidator> validators = new ArrayList<>();

    private static final FieldValidator fieldRequiredValidator = new FieldRequiredValidator();
    private static final FieldValidator integerValueValidator = new IntegerValueValidator();
    private static final FieldValidator onlyYesNoAllowed = new YesNoValidator();

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
    public ConfigurationField integerValue() {
        this.addValidator(integerValueValidator);
        return this;
    }
    public ConfigurationField yesNoValue() {
        this.addValidator(onlyYesNoAllowed);
        return this;
    }

    public ConfigurationField defaultValue(String value) {
        this.defaultValue = value;
        return this;
    }

    public ConfigurationField addValidator(FieldValidator validator) {
        this.validators.add(validator);
        return this;
    }

    public ConfigurationField setValue(String value) {
        this.value = value;
        return this;
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
                feedback.addError(String.format(VALUE_REQUIRED_FORMAT_STRING, field.label, field.name), field);
            }

            return feedback;
        }
    }

    private static class IntegerValueValidator implements FieldValidator {
        static Pattern integerPattern = Pattern.compile("^\\d*$");
        public ValidationFeedback validate(ConfigurationField field) {
            ValidationFeedback feedback = new ValidationFeedback();
            if (StringUtils.isNotEmpty(field.getValue()) && (!integerPattern.matcher(field.getValue()).matches())) {
                    feedback.addError(String.format(INTEGER_VALUE_REQUIRED_FORMAT_STRING, field.label, field.name), field);
            }

            return feedback;
        }
    }
    private static class YesNoValidator implements FieldValidator {
        static Pattern yesNoPattern = Pattern.compile("^(yes|no)$", Pattern.CASE_INSENSITIVE);
        public ValidationFeedback validate(ConfigurationField field) {
            ValidationFeedback feedback = new ValidationFeedback();
            if (StringUtils.isNotEmpty(field.getValue())) {
                if (!yesNoPattern.matcher(field.getValue()).matches()) {
                    feedback.addError(String.format(ONLY_YESNO_ALLOWED_FORMAT_STRING, field.label, field.name), field);
                } else {
                    field.setValue(field.getValue().toLowerCase());
                }
            }

            return feedback;
        }
    }
}
