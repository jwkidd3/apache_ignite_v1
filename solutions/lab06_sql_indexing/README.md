# Lab 06: SQL and Indexing - Solutions

This directory contains the solution files for Lab 6 of the Apache Ignite training course.

## Overview

Lab 6 focuses on:
- Executing SQL queries on Ignite caches (DDL, DML, DQL)
- Creating and managing indexes for query optimization
- Using JDBC driver to connect to Ignite
- Performing distributed joins
- Analyzing query execution plans

## Solution Files

| File | Description |
|------|-------------|
| `BasicSQL.java` | Exercise 1: Basic DDL, DML, DQL operations |
| `Indexing.java` | Exercise 2: Creating and testing indexes |
| `StartIgniteNode.java` | Exercise 3: Starting node for JDBC connections |
| `JDBCConnection.java` | Exercise 3: JDBC driver usage |
| `DistributedJoins.java` | Exercise 4: Distributed joins across caches |
| `QueryOptimization.java` | Optional: Query optimization and performance analysis |

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Building the Project

```bash
cd lab06_sql_indexing
mvn clean compile
```

## Running the Solutions

### Exercise 1: Basic SQL
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.BasicSQL"
```

### Exercise 2: Indexing
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.Indexing"
```

### Exercise 3: JDBC Connection

First, start the Ignite node in one terminal:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.StartIgniteNode"
```

Then run the JDBC client in another terminal:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.JDBCConnection"
```

### Exercise 4: Distributed Joins
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.DistributedJoins"
```

### Optional: Query Optimization
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.QueryOptimization"
```

## Key Concepts Demonstrated

1. **DDL Operations**: CREATE TABLE via QueryEntity
2. **DML Operations**: INSERT, UPDATE, DELETE with SqlFieldsQuery
3. **DQL Operations**: SELECT with WHERE, ORDER BY, GROUP BY
4. **Index Types**: SORTED, FULLTEXT, composite indexes
5. **JDBC Thin Driver**: Standard JDBC connectivity
6. **Distributed Joins**: Cross-cache joins with setDistributedJoins(true)
7. **Query Plans**: EXPLAIN for optimization analysis

## Index Types

| Type | Use Case | Example |
|------|----------|---------|
| SORTED | Range and equality queries | price < 100 |
| FULLTEXT | Text search | description LIKE '%wireless%' |
| COMPOSITE | Multi-column filters | category = X AND price < Y |

## Query Optimization Tips

1. Create indexes on columns used in WHERE clauses
2. Use composite indexes for multi-column filters
3. Use EXPLAIN to verify index usage
4. Avoid SELECT * - specify needed columns
5. Use parameterized queries for plan caching
6. Consider index order in composite indexes

## JDBC Connection String

```
jdbc:ignite:thin://127.0.0.1/
jdbc:ignite:thin://host1:10800,host2:10800/
jdbc:ignite:thin://host:10800/schema
```

## Troubleshooting

### Query not using index
- Verify index exists with correct columns
- Use EXPLAIN to see query plan
- Check that data types match

### JDBC connection failed
- Ensure Ignite node is running
- Check port 10800 is accessible
- Verify JDBC driver in classpath

### Distributed join very slow
- Expected behavior - requires network I/O
- Use affinity keys for better colocation
- Consider denormalization
