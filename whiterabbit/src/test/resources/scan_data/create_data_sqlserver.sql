/*
   Initialization Script for SQL Server (Azure)
   Tables: person, cost
   Can be used to manually set up an instance for testing against the standard test scan of WhiteRabbit
*/

-- 1. Clean up existing tables if they exist
DROP TABLE IF EXISTS cost;
DROP TABLE IF EXISTS person;
GO

-- 2. Create Table: person
CREATE TABLE person
(
    person_id                   INT            NOT NULL,
    gender_concept_id           INT            NOT NULL,
    year_of_birth               INT            NOT NULL,
    month_of_birth              INT            NULL,
    day_of_birth                INT            NULL,
    birth_datetime              DATETIME2      NULL, -- Changed from TIMESTAMP for SQL Server
    race_concept_id             INT            NOT NULL,
    ethnicity_concept_id        INT            NOT NULL,
    location_id                 INT            NULL,
    provider_id                 INT            NULL,
    care_site_id                INT            NULL,
    person_source_value         VARCHAR(50)    NULL,
    gender_source_value         VARCHAR(50)    NULL,
    gender_source_concept_id    INT            NULL,
    race_source_value           VARCHAR(50)    NULL,
    race_source_concept_id      INT            NULL,
    ethnicity_source_value      VARCHAR(50)    NULL,
    ethnicity_source_concept_id INT            NULL
);
GO

-- 3. Create Table: cost
CREATE TABLE cost
(
    cost_id                     INT            NOT NULL,
    cost_event_id               INT            NOT NULL,
    cost_domain_id              VARCHAR(20)    NOT NULL,
    cost_type_concept_id        INT            NOT NULL,
    currency_concept_id         INT            NULL,
    total_charge                NUMERIC(18,2)  NULL, -- Specified precision for SQL Server
    total_cost                  NUMERIC(18,2)  NULL,
    total_paid                  NUMERIC(18,2)  NULL,
    paid_by_payer               NUMERIC(18,2)  NULL,
    paid_by_patient             NUMERIC(18,2)  NULL,
    paid_patient_copay          NUMERIC(18,2)  NULL,
    paid_patient_coinsurance    NUMERIC(18,2)  NULL,
    paid_patient_deductible     NUMERIC(18,2)  NULL,
    paid_by_primary             NUMERIC(18,2)  NULL,
    paid_ingredient_cost        NUMERIC(18,2)  NULL,
    paid_dispensing_fee         NUMERIC(18,2)  NULL,
    payer_plan_period_id        INT            NULL,
    amount_allowed              NUMERIC(18,2)  NULL,
    revenue_code_concept_id     INT            NULL,
    reveue_code_source_value    VARCHAR(50)    NULL,
    drg_concept_id              INT            NULL,
    drg_source_value            VARCHAR(3)     NULL
);
GO

-- 4. Insert Data: person
INSERT INTO person (person_id, gender_concept_id, year_of_birth, month_of_birth, day_of_birth, birth_datetime, race_concept_id, ethnicity_concept_id, location_id, provider_id, care_site_id, person_source_value, gender_source_value, gender_source_concept_id, race_source_value, race_source_concept_id, ethnicity_source_value, ethnicity_source_concept_id)
VALUES
(1, 8507, 1923, 5, 1, NULL, 8527, 38003564, 1, NULL, NULL, '00013D2EFD8E45D1', '1', NULL, '1', NULL, '1', NULL),
(2, 8507, 1943, 1, 1, NULL, 8527, 38003564, 2, NULL, NULL, '00016F745862898F', '1', NULL, '1', NULL, '1', NULL),
(3, 8532, 1936, 9, 1, NULL, 8527, 38003564, 3, NULL, NULL, '0001FDD721E223DC', '2', NULL, '1', NULL, '1', NULL),
(4, 8507, 1941, 6, 1, NULL, 0, 38003563, 4, NULL, NULL, '00021CA6FF03E670', '1', NULL, '5', NULL, '5', NULL),
(5, 8507, 1936, 8, 1, NULL, 8527, 38003564, 5, NULL, NULL, '00024B3D2352D2D0', '1', NULL, '1', NULL, '1', NULL),
(6, 8507, 1943, 10, 1, NULL, 8516, 38003564, 6, NULL, NULL, '0002DAE1C81CC70D', '1', NULL, '2', NULL, '2', NULL),
(7, 8507, 1922, 7, 1, NULL, 8527, 38003564, 7, NULL, NULL, '0002F28CE057345B', '1', NULL, '1', NULL, '1', NULL),
(8, 8507, 1935, 9, 1, NULL, 8527, 38003564, 8, NULL, NULL, '000308435E3E5B76', '1', NULL, '1', NULL, '1', NULL),
(9, 8532, 1976, 9, 1, NULL, 8527, 38003564, 9, NULL, NULL, '000345A39D4157C9', '2', NULL, '1', NULL, '1', NULL),
(10, 8532, 1938, 10, 1, NULL, 8516, 38003564, 10, NULL, NULL, '00036A21B65B0206', '2', NULL, '2', NULL, '2', NULL),
(11, 8532, 1934, 2, 1, NULL, 8527, 38003564, 11, NULL, NULL, '000489E7EAAD463F', '2', NULL, '1', NULL, '1', NULL),
(12, 8507, 1929, 6, 1, NULL, 8527, 38003564, 12, NULL, NULL, '00048EF1F4791C68', '1', NULL, '1', NULL, '1', NULL),
(13, 8532, 1936, 7, 1, NULL, 8527, 38003564, 13, NULL, NULL, '0004F0ABD505251D', '2', NULL, '1', NULL, '1', NULL),
(14, 8507, 1934, 5, 1, NULL, 8527, 38003564, 14, NULL, NULL, '00052705243EA128', '1', NULL, '1', NULL, '1', NULL),
(15, 8532, 1936, 3, 1, NULL, 8527, 38003564, 15, NULL, NULL, '00070B63745BE497', '2', NULL, '1', NULL, '1', NULL),
(16, 8507, 1934, 1, 1, NULL, 8527, 38003564, 16, NULL, NULL, '0007E57CC13CE880', '1', NULL, '1', NULL, '1', NULL),
(17, 8532, 1919, 9, 1, NULL, 8516, 38003564, 17, NULL, NULL, '0007F12A492FD25D', '2', NULL, '2', NULL, '2', NULL),
(18, 8532, 1919, 10, 1, NULL, 8516, 38003564, 18, NULL, NULL, '000A005BA0BED3EA', '2', NULL, '2', NULL, '2', NULL),
(19, 8532, 1942, 7, 1, NULL, 8527, 38003564, 19, NULL, NULL, '000B4662348C35B4', '2', NULL, '1', NULL, '1', NULL),
(20, 8507, 1938, 4, 1, NULL, 8527, 38003564, 20, NULL, NULL, '000B97BA2314E971', '1', NULL, '1', NULL, '1', NULL),
(21, 8507, 1932, 8, 1, NULL, 8516, 38003564, 21, NULL, NULL, '000C7486B11E7030', '1', NULL, '2', NULL, '2', NULL),
(23, 8507, 1932, 7, 1, NULL, 8527, 38003564, 23, NULL, NULL, '000DDD364C46E2C6', '1', NULL, '1', NULL, '1', NULL),
(25, 8507, 1965, 4, 1, NULL, 8527, 38003564, 25, NULL, NULL, '00108066CA1FACCE', '1', NULL, '1', NULL, '1', NULL),
(26, 8532, 1939, 12, 1, NULL, 8527, 38003564, 26, NULL, NULL, '0010D6F80D245D62', '2', NULL, '1', NULL, '1', NULL),
(27, 8532, 1940, 4, 1, NULL, 8527, 38003564, 27, NULL, NULL, '0011714C14B52EEB', '2', NULL, '1', NULL, '1', NULL),
(28, 8507, 1937, 10, 1, NULL, 8527, 38003564, 28, NULL, NULL, '0011CB1FE23E91AF', '1', NULL, '1', NULL, '1', NULL),
(29, 8507, 1938, 4, 1, NULL, 8527, 38003564, 29, NULL, NULL, '0012AFEEC379A69D', '1', NULL, '1', NULL, '1', NULL),
(30, 8532, 1959, 11, 1, NULL, 8527, 38003564, 30, NULL, NULL, '00131C35661B2926', '2', NULL, '1', NULL, '1', NULL),
(31, 8532, 1922, 10, 1, NULL, 8527, 38003564, 31, NULL, NULL, '00139C345A104F72', '2', NULL, '1', NULL, '1', NULL),
(32, 8532, 1953, 12, 1, NULL, 8527, 38003564, 32, NULL, NULL, '0013E139F1F37264', '2', NULL, '1', NULL, '1', NULL);
GO

-- 5. Insert Data: cost
INSERT INTO cost (cost_id, cost_event_id, cost_domain_id, cost_type_concept_id, currency_concept_id, total_charge, total_cost, total_paid, paid_by_payer, paid_by_patient, paid_patient_copay, paid_patient_coinsurance, paid_patient_deductible, paid_by_primary, paid_ingredient_cost, paid_dispensing_fee, payer_plan_period_id, amount_allowed, revenue_code_concept_id, reveue_code_source_value, drg_concept_id, drg_source_value)
VALUES
(10791, 1, 'Drug', 0, 44818668, NULL, NULL, 180, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10792, 2, 'Drug', 0, 44818668, NULL, NULL, 70, NULL, 70, NULL, 70, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10793, 3, 'Drug', 0, 44818668, NULL, NULL, 60, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10794, 4, 'Drug', 0, 44818668, NULL, NULL, 130, NULL, 40, NULL, 40, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10795, 6, 'Drug', 0, 44818668, NULL, NULL, 30, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10796, 8, 'Drug', 0, 44818668, NULL, NULL, 20, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10797, 10, 'Drug', 0, 44818668, NULL, NULL, 120, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10798, 11, 'Drug', 0, 44818668, NULL, NULL, 40, NULL, 10, NULL, 10, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10799, 12, 'Drug', 0, 44818668, NULL, NULL, 110, NULL, 40, NULL, 40, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10800, 14, 'Drug', 0, 44818668, NULL, NULL, 30, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10801, 18, 'Drug', 0, 44818668, NULL, NULL, 0, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10802, 19, 'Drug', 0, 44818668, NULL, NULL, 10, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10803, 21, 'Drug', 0, 44818668, NULL, NULL, 30, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10804, 25, 'Drug', 0, 44818668, NULL, NULL, 20, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10805, 27, 'Drug', 0, 44818668, NULL, NULL, 20, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10806, 28, 'Drug', 0, 44818668, NULL, NULL, 0, NULL, 10, NULL, 10, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10807, 29, 'Drug', 0, 44818668, NULL, NULL, 30, NULL, 10, NULL, 10, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10808, 31, 'Drug', 0, 44818668, NULL, NULL, 350, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10809, 33, 'Drug', 0, 44818668, NULL, NULL, 10, NULL, 10, NULL, 10, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10810, 35, 'Drug', 0, 44818668, NULL, NULL, 570, NULL, 80, NULL, 80, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10811, 37, 'Drug', 0, 44818668, NULL, NULL, 0, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10812, 38, 'Drug', 0, 44818668, NULL, NULL, 150, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10813, 41, 'Drug', 0, 44818668, NULL, NULL, 0, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10814, 42, 'Drug', 0, 44818668, NULL, NULL, 20, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10815, 45, 'Drug', 0, 44818668, NULL, NULL, 70, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10816, 51, 'Drug', 0, 44818668, NULL, NULL, 80, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10817, 52, 'Drug', 0, 44818668, NULL, NULL, 120, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10818, 53, 'Drug', 0, 44818668, NULL, NULL, 70, NULL, 70, NULL, 70, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10819, 55, 'Drug', 0, 44818668, NULL, NULL, 0, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10820, 56, 'Drug', 0, 44818668, NULL, NULL, 70, NULL, 170, NULL, 170, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10821, 58, 'Drug', 0, 44818668, NULL, NULL, 70, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10822, 61, 'Drug', 0, 44818668, NULL, NULL, 160, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10823, 62, 'Drug', 0, 44818668, NULL, NULL, 30, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL),
(10824, 63, 'Drug', 0, 44818668, NULL, NULL, 350, NULL, 10, NULL, 10, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL);
GO