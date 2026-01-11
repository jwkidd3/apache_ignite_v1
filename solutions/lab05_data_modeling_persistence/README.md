# Lab 05: Data Modeling and Persistence - Solutions

This directory contains the solution files for Lab 5 of the Apache Ignite training course.

## Overview

Lab 5 focuses on:
- Data modeling best practices for Ignite
- Implementing affinity keys for data colocation
- Configuring native persistence layer
- Implementing write-through, write-behind, and read-through patterns

## Solution Files

| File | Description |
|------|-------------|
| `model/Customer.java` | Exercise 1: Customer domain model |
| `model/Order.java` | Exercise 1: Order domain model with @AffinityKeyMapped |
| `AffinityKeys.java` | Exercise 2: Affinity colocation demonstration |
| `Persistence.java` | Exercise 3: Native persistence configuration |
| `CacheStore.java` | Exercise 4: Read-through/Write-through cache store |
| `WriteBehind.java` | Exercise 5: Write-behind cache store |

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Quick Start

```bash
# Build
mvn clean compile

# Package (create JAR with dependencies)
mvn clean package

# Run a specific solution
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.AffinityKeys"
```

## All Maven Commands

```bash
# Clean the project
mvn clean

# Compile only
mvn compile

# Package into JAR
mvn package

# Skip tests during package
mvn package -DskipTests

# Download dependencies
mvn dependency:resolve

# Copy dependencies to target/dependency
mvn dependency:copy-dependencies

# Run with custom JVM options
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.AffinityKeys" -Dexec.args="" \
  -Djava.net.preferIPv4Stack=true
```

## Running the Solutions

### Exercise 1-2: Affinity Keys
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.AffinityKeys"
```

### Exercise 3: Native Persistence
```bash
# Run once to create data
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.Persistence"

# Run again to verify data recovery
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.Persistence"
```

### Exercise 4: Cache Store (Read-Through/Write-Through)
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.CacheStore"
```

### Exercise 5: Write-Behind
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.WriteBehind"
```

## All Solution Run Commands

```bash
# Affinity Keys demonstration
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.AffinityKeys"

# Native Persistence
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.Persistence"

# Cache Store (Read-Through/Write-Through)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.CacheStore"

# Write-Behind
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.WriteBehind"
```

## Running Without Maven

```bash
# After running 'mvn package dependency:copy-dependencies'
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab05.AffinityKeys

# With JVM options
java -Xms512m -Xmx2g -Djava.net.preferIPv4Stack=true \
  -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab05.AffinityKeys

# Run Persistence
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab05.Persistence

# Run CacheStore
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab05.CacheStore

# Run WriteBehind
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab05.WriteBehind
```

## Key Concepts Demonstrated

1. **Affinity Keys**: @AffinityKeyMapped ensures related data colocation
2. **Data Colocation**: Related entities stored on same node for efficient joins
3. **Native Persistence**: Data survives restarts with WAL
4. **Cache Store Patterns**:
   - Read-Through: Load from DB on cache miss
   - Write-Through: Synchronous write to DB
   - Write-Behind: Asynchronous batched writes

## Cleanup

To clean up persistence data between runs:
```bash
rm -rf ./ignite-data ./ignite-wal ./ignite-wal-archive
```

## Affinity Key Best Practices

- Use affinity keys for parent-child relationships
- Ensure consistent data types for affinity keys
- Consider data distribution when choosing affinity keys
- Test colocation with Affinity.mapKeyToNode()

## Cache Store Patterns Comparison

| Pattern | Consistency | Latency | Use Case |
|---------|-------------|---------|----------|
| Write-Through | Immediate | Higher | Financial data |
| Write-Behind | Eventual | Lower | High-throughput writes |
| Read-Through | On-demand | Cache miss penalty | Read-heavy workloads |

## Troubleshooting

### Data not colocated
- Verify @AffinityKeyMapped annotation
- Check that affinity key field matches
- Ensure consistent cache configuration

### Persistence not working
- Must activate cluster for persistent regions
- Check file permissions on storage paths
- Verify persistence enabled in data region config

### Write-behind not flushing
- Check flush frequency and size configuration
- Ensure cache store is properly configured
- Look for exceptions in logs
