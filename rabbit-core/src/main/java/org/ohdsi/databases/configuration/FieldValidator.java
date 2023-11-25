package org.ohdsi.databases.configuration;

@FunctionalInterface
public interface FieldValidator {
    ValidationFeedback validate(ConfigurationField field);
}
