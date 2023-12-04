/*******************************************************************************
 * Copyright 2019 Observational Health Data Sciences and Informatics
 * 
 * This file is part of WhiteRabbit
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.databases.configuration;

import org.ohdsi.databases.DBConnectionInterface;
import org.ohdsi.databases.SnowflakeConnection;

import java.util.stream.Stream;

public enum DbType {
	/*
	 * Please note: the names and strings and the Type enum below must match when String.toUpperCase().replace(" ", "_")
	 * is applied (see constructor and the normalizedName() method). This is enforced when the enum values are constructed,
	 * and a violation of this rule will result in a DBConfigurationException being thrown.
	 */
	DELIMITED_TEXT_FILES("Delimited text files"),
	MYSQL("MySQL"),
	ORACLE("Oracle"),
	SQL_SERVER("SQL Server"),
	POSTGRESQL("PostgreSQL"),
	MS_ACCESS("MS Access"),
	PDW("PDW"),
	REDSHIFT("Redshift"),
	TERADATA("Teradata"),
	BIGQUERY("BigQuery"),
	AZURE("Azure"),
	SNOWFLAKE("Snowflake", SnowflakeConnection.INSTANCE),
	SAS7BDAT("Sas7bdat");

	private final String label;
	private final DBConnectionInterface implementingClass;

	DbType(String type) {
		this(type, null);
	}

	DbType(String label, DBConnectionInterface implementingClass) {
		this.label = label;
		this.implementingClass = implementingClass;
		if (!this.name().equals(normalizedName(label))) {
			throw new DBConfigurationException(String.format(
					"%s: the normalized value of label '%s' (%s) must match the name of the enum constant (%s)",
					DbType.class.getName(),
					label,
					normalizedName(label),
					this.name()
			));
		}
	}

	public boolean equalsDbType(DbType other) {
		return (other != null && other.equals(this));
	}

	public boolean supportsDBConnectorInterface() {
		return this.implementingClass != null;
	}

	public DBConnectionInterface getDbConnectorInterface() throws DBConfigurationException {
		if (this.supportsDBConnectorInterface()) {
			return this.implementingClass;
		} else {
			throw new DBConfigurationException(String.format("Class %s does not implement interface %s",
					this.implementingClass.getClass().getName(),
					DBConnectionInterface.class.getName()));
		}
	}

	public static DbType getDbType(String name) {
		return DbType.valueOf(DbType.class, normalizedName(name));
	}

	/**
	 * Returns the list of supported database in the order that they should appear in the GUI.
	 *
	 * @return Array of labels for the supported database, intended for use in a selector (like a Swing JComboBox)
	 */
	public static String[] pickList() {
		return Stream.of(DELIMITED_TEXT_FILES, SAS7BDAT, MYSQL, ORACLE, SQL_SERVER, POSTGRESQL, MS_ACCESS, PDW, REDSHIFT, TERADATA, BIGQUERY, AZURE, SNOWFLAKE)
				.map(DbType::label).toArray(String[]::new);
	}

	public String label() {
		return this.label;
	}

	private static String normalizedName(String name) {
		return name.toUpperCase().replace(" ", "_");
	}
}
