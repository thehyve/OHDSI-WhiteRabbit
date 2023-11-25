package org.ohdsi.databases.configuration;

@FunctionalInterface
public interface ConfigurationValidator {
    ValidationFeedback validate(ConfigurationFields fields);
}
