package org.ohdsi.databases.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ConfigurationFields {
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
