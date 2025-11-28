package org.ohdsi.databases;
import org.apache.commons.lang.StringUtils;
import org.ohdsi.databases.configuration.*;
import org.ohdsi.utilities.collections.Pair;
import org.ohdsi.utilities.files.IniFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.ohdsi.databases.AzureSqlEntraHandler.AzureSqlConfiguration.*;

/**
 * AzureSqlEntraHandler provides JdbcStorageHandler implementation for Azure SQL using Entra ID (Azure AD) authentication.
 * It acquires an access token via Azure Identity and passes it to the Microsoft JDBC driver.
 */
public enum AzureSqlEntraHandler implements JdbcStorageHandler {
    INSTANCE();

    private static final String MSSQL_JDBC_CLASSNAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String AZURE_SQL_SCOPE = "https://database.windows.net/.default";

    private DBConnection connection;
    private static final ScanConfiguration configuration = new AzureSqlConfiguration();

    @Override
    public JdbcStorageHandler getInstance(DbSettings dbSettings) {
        if (connection == null) {
            connection = connect(dbSettings);
        }
        return INSTANCE;
    }

    @Override
    public DBConnection getDBConnection() {
        checkInitialised();
        return connection;
    }

    @Override
    public DbType getDbType() {
        return DbType.AZURE_ENTRA;
    }

    @Override
    public void checkInitialised() throws ScanConfigurationException {
        if (this.connection == null) {
            throw new ScanConfigurationException("Azure SQL DB/connection was not initialized");
        }
    }

    @Override
    public String getDatabase() {
        return configuration.getValue(AZURE_SQL_DATABASE);
    }

    public String getSchema() {
        return configuration.getValue(AZURE_SQL_SCHEMA);
    }

    @Override
    public String getTablesQuery(String database) {
        // Return fully-qualified table names as a single column result
        return String.format(
                "SELECT (TABLE_SCHEMA + '.' + TABLE_NAME) AS TABLE_NAME FROM %s.INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE'",
                bracket(database));
    }

    @Override
    public String getFieldsInformationQuery(String table) {
        // Expect input table as plain name without schema
        String[] parts = resolveTableName(table).split("\\.");
        String schema = unbracket(parts[0]);
        String name = unbracket(parts[1]);
        return String.format("SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='%s' AND TABLE_NAME='%s'",
                schema, name);
    }

    @Override
    public String getRowSampleQuery(String tableName, long rowCount, long sampleSize) {
        String full = resolveTableName(tableName);
        if (sampleSize <= 0) {
            return String.format("SELECT * FROM %s", full);
        }
        // Use ORDER BY NEWID() for a random sample that works consistently on Azure SQL
        return String.format("SELECT TOP %d * FROM %s ORDER BY NEWID()", sampleSize, full);
    }

    @Override
    public String getTableSizeQuery(String tableName) {
        return String.format("SELECT COUNT(*) FROM %s;", resolveTableName(tableName));
    }

    @Override
    public int getTableNameIndex() {
        return 0;
    }

    @Override
    public ScanConfiguration getScanConfiguration() {
        return configuration;
    }

    private DBConnection connect(DbSettings dbSettings) {
        try {
            Class.forName(MSSQL_JDBC_CLASSNAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find JDBC driver. Ensure 'mssql-jdbc' is on the classpath: " + e.getMessage());
        }

        String server = configuration.getValue(AZURE_SQL_SERVER);
        String database = configuration.getValue(AZURE_SQL_DATABASE);

        String url = String.format(
                "jdbc:sqlserver://%s;database=%s;encrypt=true;hostNameInCertificate=*.database.windows.net;loginTimeout=30",
                server, database);

        String accessToken = acquireAccessToken();
        Properties props = new Properties();
        props.setProperty("accessToken", accessToken);

        try {
            Connection c = DriverManager.getConnection(url, props);
            return new DBConnection(c, DbType.AZURE_ENTRA, true);
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("is not currently available.  please retry the connection later.")) {
                logger.warn("Please note that the exception thrown may be due to the database having been paused. Repeating this test a few seconds or minutes later may succeed.");
            }
            throw new RuntimeException("Cannot connect to Azure SQL server: " + e.getMessage());
        }
    }

    private String acquireAccessToken() {
        String authMethod = configuration.getValue(AZURE_SQL_AUTH_METHOD);
        try {
            // Build TokenRequestContext via reflection
            Class<?> ctxClass = Class.forName("com.azure.core.credential.TokenRequestContext");
            Object ctx = ctxClass.getDeclaredConstructor().newInstance();
            ctxClass.getMethod("addScopes", String[].class).invoke(ctx, (Object) new String[]{AZURE_SQL_SCOPE});

            Object credential;
            if ("client_secret".equalsIgnoreCase(authMethod)) {
                String tenantId = configuration.getValue(AZURE_SQL_TENANT_ID);
                String clientId = configuration.getValue(AZURE_SQL_CLIENT_ID);
                String clientSecret = configuration.getValue(AZURE_SQL_CLIENT_SECRET);

                Class<?> builderClass = Class.forName("com.azure.identity.ClientSecretCredentialBuilder");
                Object builder = builderClass.getDeclaredConstructor().newInstance();
                builderClass.getMethod("tenantId", String.class).invoke(builder, tenantId);
                builderClass.getMethod("clientId", String.class).invoke(builder, clientId);
                builderClass.getMethod("clientSecret", String.class).invoke(builder, clientSecret);
                credential = builderClass.getMethod("build").invoke(builder);
            } else {
                Class<?> builderClass = Class.forName("com.azure.identity.DefaultAzureCredentialBuilder");
                Object builder = builderClass.getDeclaredConstructor().newInstance();
                credential = builderClass.getMethod("build").invoke(builder);
            }

            // Call getToken(ctx).block() and extract token string
            Object mono = credential.getClass().getMethod("getToken", ctxClass).invoke(credential, ctx);
            Object accessToken = mono.getClass().getMethod("block").invoke(mono);
            if (accessToken == null) {
                throw new RuntimeException("Failed to acquire access token (null)");
            }
            String token = (String) accessToken.getClass().getMethod("getToken").invoke(accessToken);
            if (StringUtils.isEmpty(token)) {
                throw new RuntimeException("Failed to acquire access token (empty)");
            }
            return token;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Azure Identity libraries not found on classpath. Add 'com.azure:azure-identity' to runtime.");
        } catch (Exception e) {
            throw new RuntimeException("Error acquiring Azure access token: " + e.getMessage(), e);
        }
    }

    private static String resolveTableName(String tableName) {
        // If caller passed a fully qualified name, respect it; otherwise, prefix configured schema
        if (tableName.contains(".")) {
            return bracketPath(tableName);
        }
        String schema = configuration.getValue(AZURE_SQL_SCHEMA);
        return String.format("[%s].[%s]", schema, tableName);
    }

    private static String bracketPath(String name) {
        String[] parts = name.split("\\.");
        if (parts.length == 2) {
            return String.format("[%s].[%s]", unbracket(parts[0]), unbracket(parts[1]));
        }
        return name;
    }

    private static String bracket(String identifier) {
        if (identifier == null) return null;
        String id = identifier;
        if (id.startsWith("[") && id.endsWith("]")) return id; // already bracketed
        return String.format("[%s]", id);
    }

    private static String unbracket(String identifier) {
        if (identifier == null) return null;
        if (identifier.startsWith("[") && identifier.endsWith("]")) {
            return identifier.substring(1, identifier.length() - 1);
        }
        return identifier;
    }

    /**
     * Configuration for Azure SQL (Entra ID) connections.
        */
    public static class AzureSqlConfiguration extends ScanConfiguration {
        public static final String AZURE_SQL_SERVER = "AZURE_SQL_SERVER";
        public static final String AZURE_SQL_DATABASE = "AZURE_SQL_DATABASE";
        public static final String AZURE_SQL_SCHEMA = "AZURE_SQL_SCHEMA";
        public static final String AZURE_SQL_AUTH_METHOD = "AZURE_SQL_AUTH_METHOD"; // default | client_secret
        public static final String AZURE_SQL_TENANT_ID = "AZURE_SQL_TENANT_ID";
        public static final String AZURE_SQL_CLIENT_ID = "AZURE_SQL_CLIENT_ID";
        public static final String AZURE_SQL_CLIENT_SECRET = "AZURE_SQL_CLIENT_SECRET";

        public AzureSqlConfiguration() {
            super(
                    ConfigurationField.create(AZURE_SQL_SERVER, "Server", "Fully qualified Azure SQL server hostname, e.g. yourserver.database.windows.net").required(),
                    ConfigurationField.create(AZURE_SQL_DATABASE, "Database", "Database name").required(),
                    ConfigurationField.create(AZURE_SQL_SCHEMA, "Schema", "Default schema for tables").defaultValue("dbo").required(),
                    ConfigurationField.create(AZURE_SQL_AUTH_METHOD, "Authentication method", "Entra ID authentication method: 'default' or 'client_secret'")
                            .defaultValue("default")
                            .addValidator(field -> {
                                ValidationFeedback fb = new ValidationFeedback();
                                List<String> allowed = Arrays.asList("default", "client_secret");
                                if (StringUtils.isNotEmpty(field.getValue()) && !allowed.contains(field.getValue().toLowerCase())) {
                                    fb.addError("Authentication method must be one of: default, client_secret", field);
                                }
                                return fb;
                            }),
                    ConfigurationField.create(AZURE_SQL_TENANT_ID, "Tenant ID", "Directory (tenant) ID for client credentials flow"),
                    ConfigurationField.create(AZURE_SQL_CLIENT_ID, "Client ID", "Application (client) ID for client credentials flow"),
                    ConfigurationField.create(AZURE_SQL_CLIENT_SECRET, "Client Secret", "Client secret for client credentials flow")
            );

            // Cross-field validation for client_secret method
            this.configurationFields.addValidator(fields -> {
                ValidationFeedback fb = new ValidationFeedback();
                String method = fields.getValue(AZURE_SQL_AUTH_METHOD);
                if ("client_secret".equalsIgnoreCase(method)) {
                    if (StringUtils.isEmpty(fields.getValue(AZURE_SQL_TENANT_ID))) {
                        fb.addError("TENANT_ID is required for client_secret auth", fields.get(AZURE_SQL_TENANT_ID));
                    }
                    if (StringUtils.isEmpty(fields.getValue(AZURE_SQL_CLIENT_ID))) {
                        fb.addError("CLIENT_ID is required for client_secret auth", fields.get(AZURE_SQL_CLIENT_ID));
                    }
                    if (StringUtils.isEmpty(fields.getValue(AZURE_SQL_CLIENT_SECRET))) {
                        fb.addError("CLIENT_SECRET is required for client_secret auth", fields.get(AZURE_SQL_CLIENT_SECRET));
                    }
                }
                return fb;
            });
        }

        @Override
        public DbSettings toDbSettings(ValidationFeedback feedback) {
            DbSettings s = new DbSettings();
            s.dbType = DbType.AZURE_ENTRA;
            s.server = this.getValue(AZURE_SQL_SERVER);
            s.database = this.getValue(AZURE_SQL_DATABASE);
            s.domain = this.getValue(AZURE_SQL_SCHEMA);
            s.user = "EntraID"; // informational only, token-based
            s.sourceType = DbSettings.SourceType.DATABASE;
            return s;
        }
    }

    public static Pair<AzureSqlConfiguration, DbSettings> getConfiguration(IniFile iniFile, ValidationFeedback feedback) {
        AzureSqlConfiguration c = new AzureSqlConfiguration();
        ValidationFeedback current = c.loadAndValidateConfiguration(iniFile);
        if (feedback != null) {
            feedback.add(current);
        }
        DbSettings s = c.toDbSettings(feedback);
        return new Pair<>(c, s);
    }
}
