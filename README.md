![WR logo](https://github.com/OHDSI/WhiteRabbit/blob/master/whiterabbit/src/main/resources/org/ohdsi/whiteRabbit/WhiteRabbit64.png) White Rabbit 
===========

![RiaH logo](https://github.com/OHDSI/WhiteRabbit/blob/master/rabbitinahat/src/main/resources/org/ohdsi/rabbitInAHat/RabbitInAHat64.png) Rabbit in a Hat
===========

Introduction
========
**WhiteRabbit** is a small application that can be used to analyse the structure and contents of a database as preparation for designing an ETL. It comes with **RabbitInAHat**, an application for interactive design of an ETL to the OMOP Common Data Model with the help of the the scan report generated by White Rabbit. 

Features
========
- Can scan databases in SQL Server, Oracle, PostgreSQL, MySQL, MS Access, Amazon RedShift, Google BigQuery, SAS files and CSV files
- The scan report contains information on tables, fields, and frequency distributions of values
- Cutoff on the minimum frequency of values to protect patient privacy
- WhiteRabbit can be run with a graphical user interface or from the command prompt
- Interactive tool (Rabbit in a Hat) for designing the ETL using the scan report as basis
- Rabbit in a Hat generates ETL specification document according to OMOP template

Screenshots
===========
<table border = "">
<tr valign="top">
<td width = 50%>
  <img src="https://github.com/OHDSI/WhiteRabbit/blob/master/docs/images/WRScreenshot.png" alt="White Rabbit" title="White Rabbit" />
</td>
<td width = 50%>
 <img src="https://github.com/OHDSI/WhiteRabbit/blob/master/docs/images/RIAHScreenshot.png" alt="Rabbit in a Hat" title="Rabbit in a Hat" />
</td>
</tr><tr>
<td>White Rabbit</td><td>Rabbit in a Hat</td>
</tr>
</table>

Technology
============
White Rabbit and Rabbit in a Hat are pure Java applications. Both applications use [Apache's POI Java libraries](http://poi.apache.org/) to read and write Word and Excel files. White Rabbit uses JDBC to connect to the respective databases.

System Requirements
============
Requires Java 1.8 or higher, and read access to the database to be scanned. Java can be downloaded from
<a href="http://www.java.com" target="_blank">http://www.java.com</a>.

Dependencies
============
For the distributable packages, the only requirement is Java 8. For building the package, also Maven is needed.

Getting Started
===============
WhiteRabbit

1. Under the [Releases](https://github.com/OHDSI/WhiteRabbit/releases) tab, download `WhiteRabbit*.zip`
2. Unzip the download
3. Double-click on `bin/whiteRabbit.bat` on Windows to start White Rabbit, and `bin/whiteRabbit` on macOS and Linux.

(See [the documentation](http://ohdsi.github.io/WhiteRabbit/WhiteRabbit.html#running-from-the-command-line) for details on how to run from the command prompt instead)

Rabbit-In-A-Hat

1. Using the files downloaded for WhiteRabbit, double-click on `bin/rabbitInAHat.bat` to start Rabbit-In-A-Hat on Windows, and `bin/rabbitInAHat` on macOS and Linux.

Note: on releases earlier than version 0.8.0, open the respective `WhiteRabbit.jar` or `RabbitInAHat.jar` files instead.

Example in- and output
========
The file `examples.zip` contains a set of input and output examples for White Rabbit and Rabbit in a Hat.
These are used for testing of the main White Rabbit and Rabbit in a Hat features. An overview is given in below table.

| Folder         | Description                                                                                                                                                                                                                                                                                                                                              |
|:---------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `wr_input_csv` | csv files to test scanning on different data types and long table names.                                                                                                                                                                                                                                                                                 |
| `wr_input_sas` | sas7bdat files to test sas input                                                                                                                                                                                                                                                                                                                         |
| `wr_output`    | Scan reports created from files in `wr_input_csv`, `wr_input_sas` and [native a Synthea database loaded in Postgres](https://github.com/ohdsi/ETL-Synthea). All with default scan options.<br> This folder also includes fake data generated from the csv scan report. The csv scan report is used to test the opening a Scan Report in Rabbit in a Hat. |
| `riah_input`   | An example mapping file used to create the Rabbit in a Hat outputs.                                                                                                                                                                                                                                                                                      |
| `riah_output`  | All export formats created by Rabbit in a Hat: as word, html, markdown, sql skeleton and the R TestFramework.<br> These are all generated from `riah_input/riah_mapping_example.gz`.                                                                                                                                                                     |


Getting Involved
=============
* User guide and Help: [WhiteRabbit documentation](http://ohdsi.github.io/WhiteRabbit)
* Developer questions/comments/feedback: [OHDSI Forum](http://forums.ohdsi.org/c/developers)
* We use the [GitHub issue tracker](../../issues) for all bugs/issues/enhancements
* Historically, all files have CRLF line endings. Please configure your IDE and local git to keep line endings as is. This avoids merge conflicts.

License
=======
WhiteRabbit is licensed under Apache License 2.0

Development
===========
White Rabbit and Rabbit in a Hat are structured as a Maven package and can be developed in Eclipse. Contributions are welcome.

While the software in the project can be executed with Java 1.8, for development Java 17 is needed.
This has to do with test and verification dependencies that are not available in
a version compatible with Java 1.8 .

To generate the files ready for distribution, run `mvn install`.

### Testing

Some newer code has unit and/or integration tests. Tests that depend on external resources being available,
such as a database, should be excluded from executing automatically. You can use the @Tag annotation to combine
such tests in a group with the same tag, and exclude that tag from being run automatically by maven (this
should be done in the configuration of the surefire plugin in pom.xml of the module involved).

An exception to the above are tests that depend on Docker. Currently, these tests are implemented with the 
TestContainers library, and are configured to check for Docker being present. If not, these tests will not
be run, but the tests as a whole will still succeed. However, it is recommended that these tests are run since
these tests verify essential functionality for WhiteRabbit, like the database interface.

Also, GitHub actions have been configured to run the test suite automatically.

#### Snowflake

There are automated tests for Snowflake, but since it is not (yet?) possible to have a local
Snowflake instance in a Docker container, these test will only run if the following information
is provided through environment variables:

    SNOWFLAKE_WR_TEST_ACCOUNT
    SNOWFLAKE_WR_TEST_USER
    SNOWFLAKE_WR_TEST_PASSWORD
    SNOWFLAKE_WR_TEST_WAREHOUSE
    SNOWFLAKE_WR_TEST_DATABASE
    SNOWFLAKE_WR_TEST_SCHEMA

It is recommended that user, password, database and schema are created for these tests only,
and do not relate in any way to any production environment. 
The schema should not contain any tables. 

### Development status

Production. This program is being used by many people.
