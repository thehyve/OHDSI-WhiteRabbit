package org.ohdsi.databases;

import org.ohdsi.utilities.files.IniFile;

public interface IniFileHandler {
    /*static DbSettings interpretIniFile(IniFile iniFile) {
        if ()
        if (this.getClass().isInstance(IniFileHandler.class)) {
            return this.interpretIniFile(iniFile);
        } else {
            return IniFileHandler.interpretIniFileClassic(iniFile);
        }
    }*/

    default boolean handlesIniFile() {
        return false;
    }

    static DbSettings interpretIniFileClassic(IniFile iniFile) {
        DbSettings dbSettings = new DbSettings();
        if (iniFile.get("DATA_TYPE").equalsIgnoreCase(DbType.DELIMITED_TEXT_FILES.getTypeName())) {
            dbSettings.sourceType = DbSettings.SourceType.CSV_FILES;
            if (iniFile.get("DELIMITER").equalsIgnoreCase("tab"))
                dbSettings.delimiter = '\t';
            else
                dbSettings.delimiter = iniFile.get("DELIMITER").charAt(0);
        } else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("SAS7bdat")) {
            dbSettings.sourceType = DbSettings.SourceType.SAS_FILES;
        } else {
            dbSettings.sourceType = DbSettings.SourceType.DATABASE;
            dbSettings.user = iniFile.get("USER_NAME");
            dbSettings.password = iniFile.get("PASSWORD");
            dbSettings.server = iniFile.get("SERVER_LOCATION");
            dbSettings.database = iniFile.get("DATABASE_NAME");
            if (iniFile.get("DATA_TYPE").equalsIgnoreCase("MySQL"))
                dbSettings.dbType = DbType.MYSQL;
            else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("Oracle"))
                dbSettings.dbType = DbType.ORACLE;
            else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("PostgreSQL"))
                dbSettings.dbType = DbType.POSTGRESQL;
            else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("Redshift"))
                dbSettings.dbType = DbType.REDSHIFT;
            else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("SQL Server")) {
                dbSettings.dbType = DbType.MSSQL;
                if (!iniFile.get("USER_NAME").isEmpty()) { // Not using windows authentication
                    String[] parts = iniFile.get("USER_NAME").split("/");
                    if (parts.length == 2) {
                        dbSettings.user = parts[1];
                        dbSettings.domain = parts[0];
                    }
                }
            } else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("Azure")) {
                dbSettings.dbType = DbType.AZURE;
                if (!iniFile.get("USER_NAME").isEmpty()) { // Not using windows authentication
                    String[] parts = iniFile.get("USER_NAME").split("/");
                    if (parts.length == 2) {
                        dbSettings.user = parts[1];
                        dbSettings.domain = parts[0];
                    }
                }
            } else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("PDW")) {
                dbSettings.dbType = DbType.PDW;
                if (!iniFile.get("USER_NAME").isEmpty()) { // Not using windows authentication
                    String[] parts = iniFile.get("USER_NAME").split("/");
                    if (parts.length == 2) {
                        dbSettings.user = parts[1];
                        dbSettings.domain = parts[0];
                    }
                }
            } else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("MS Access"))
                dbSettings.dbType = DbType.MSACCESS;
            else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("Teradata"))
                dbSettings.dbType = DbType.TERADATA;
            else if (iniFile.get("DATA_TYPE").equalsIgnoreCase("BigQuery")) {
                dbSettings.dbType = DbType.BIGQUERY;
                /* GBQ requires database. Putting database into domain var for connect() */
                dbSettings.domain = dbSettings.database;
            }
        }
        return dbSettings;
    }
}
