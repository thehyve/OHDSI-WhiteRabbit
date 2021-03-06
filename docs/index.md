---
layout: default
title: Introduction
nav_order: 1
permalink: /
---

# ![WR logo](https://github.com/OHDSI/WhiteRabbit/raw/master/whiterabbit/src/main/resources/org/ohdsi/whiteRabbit/WhiteRabbit64.png) White Rabbit

# ![RiaH logo](https://github.com/OHDSI/WhiteRabbit/raw/master/rabbitinahat/src/main/resources/org/ohdsi/rabbitInAHat/RabbitInAHat64.png) Rabbit in a Hat


## Introduction
**WhiteRabbit** is a small application that can be used to analyse the structure and contents of a database as preparation for designing an ETL.
It comes with **RabbitInAHat**, an application for interactive design of an ETL to the OMOP Common Data Model with the help of the the scan report generated by White Rabbit.

## Features
- Can scan databases in SQL Server, Oracle, PostgreSQL, MySQL, MS Access, Teradata, PDW, Amazon RedShift, Google BigQuery, SAS files and CSV files
- The scan report contains information on tables, fields, and frequency distributions of values
- Cutoff on the minimum frequency of values to protect patient privacy
- WhiteRabbit can be run with a graphical user interface or from the command prompt
- Interactive tool (Rabbit in a Hat) for designing the ETL using the scan report as basis
- Rabbit in a Hat generates ETL specification document according to OMOP templatement according to OMOP template

## Current version
v0.9.0, https://github.com/OHDSI/WhiteRabbit/releases/tag/v0.9.0