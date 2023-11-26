package org.ohdsi.databases.configuration;

import org.ohdsi.databases.DBConnectorInterface;
import org.ohdsi.databases.SnowflakeConnector;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum DBChoice {

    DelimitedTextFiles("Delimited text files"),
    SAS7bdat("SAS7bdat"),
    MySQL("MySQL"),
    Oracle("Oracle"),
    SQLServer("SQL Server"),
    PostgreSQL("PostgreSQL"),
    MSAccess("MS Access"),
    PDW("PDW"),
    Redshift("Redshift"),
    Teradata("Teradata"),
    BigQuery("BigQuery"),
    Azure("Azure"),
    Snowflake("Snowflake", SnowflakeConnector.INSTANCE);

    private final String name;
    private final DBConnectorInterface implementingClass;

    DBChoice(String s) {
        this(s, null);
    }
    DBChoice(String s, DBConnectorInterface implementingClass) {
        this.name = s;
        this.implementingClass = implementingClass;
    }

    public boolean equalsName(String otherName) {
        // (otherName == null) check is not needed because name.equals(null) returns false
        return name.equals(otherName);
    }

    public boolean supportsDBConnectorInterface() {
        return (this.implementingClass != null);
    }

    public DBConnectorInterface getDbConnectorInterface() throws DBConfigurationException {
        if (this.supportsDBConnectorInterface()) {
            return this.implementingClass;
        } else {
            throw new DBConfigurationException(String.format("Class %s does not implement interface %s",
                    this.implementingClass.getClass().getName(),
                    DBConnectorInterface.class.getName()));
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static String[] choices() {
        return Arrays.stream(DBChoice.values()).map(Enum::toString).toArray(String[]::new);
    }

    public static DBChoice getDBChoice(String choice) {
        Optional<DBChoice> chosen = Arrays.stream(DBChoice.values()).filter(c -> c.name.equalsIgnoreCase(choice)).findFirst();
        if (chosen.isPresent()) {
            return chosen.get();
        } else {
            throw new DBConfigurationException(String.format("Unknown name for enum %s: '%s'", DBChoice.class.getName(), choice));
        }
    }
}
