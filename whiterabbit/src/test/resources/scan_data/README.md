The ScanReport-reference-v0.10.7-{csv,sql}.xlsx files in this directory were generated using the last version
of WhiteRabbit that did not have any unit or integration tests, and serve as the reference for smoke/regression
tests.

The ScanReport-v0.10.7-reference-100000000-2147483647-stats-seed-556365.xlsx file was also generated with the 
"last know good" version of WhiteRabbit, and serves as a reference for the maximum number of records that
WhiteRabbit v0.10.7 could handle. 

Not that the order in which files/tables are generated into these xlsx files was (is) not entirely predictable,
so some sorting is done in the tests to match the version under test.

