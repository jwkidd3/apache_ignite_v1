# Lab 07: Transactions and ACID Properties - Solutions

## Overview

This lab covers transaction concepts in Apache Ignite, including:
- PESSIMISTIC and OPTIMISTIC transaction models
- Different isolation levels (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE)
- Deadlock detection and handling
- Cross-cache atomic transactions
- Transaction best practices

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Project Structure

```
lab07_transactions_acid/
├── pom.xml
├── README.md
└── src/main/java/com/example/ignite/solutions/lab07/
    ├── Lab07BasicTransactions.java      - Exercise 1: Basic transactional operations
    ├── Lab07TransactionModels.java      - Exercise 2: PESSIMISTIC vs OPTIMISTIC
    ├── Lab07BestPractices.java          - Exercise 3: Transaction best practices
    ├── Lab07IsolationComparison.java    - Exercise 4: Isolation level comparison
    ├── Lab07DeadlockHandling.java       - Exercise 5: Deadlock handling and retry
    ├── Lab07CrossCacheTransactions.java - Exercise 6: Cross-cache transactions
    ├── Lab07BankTransfer.java           - Challenge 1: Bank transfer system
    └── Lab07TransactionMonitor.java     - Challenge 2: Transaction monitoring
```

## Quick Start

```bash
# Build
mvn clean compile

# Package (create JAR with dependencies)
mvn clean package

# Run a specific solution
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BasicTransactions"
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
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BasicTransactions" -Dexec.args="" \
  -Djava.net.preferIPv4Stack=true
```

## Running the Solutions

### Exercise 1: Basic Transactions
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BasicTransactions"
```

### Exercise 2: Transaction Models
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07TransactionModels"
```

### Exercise 3: Best Practices
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BestPractices"
```

### Exercise 4: Isolation Comparison
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07IsolationComparison"
```

### Exercise 5: Deadlock Handling
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07DeadlockHandling"
```

### Exercise 6: Cross-Cache Transactions
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07CrossCacheTransactions"
```

### Challenge 1: Bank Transfer
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BankTransfer"
```

### Challenge 2: Transaction Monitor
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07TransactionMonitor"
```

## All Solution Run Commands

```bash
# Basic Transactions
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BasicTransactions"

# Transaction Models (PESSIMISTIC vs OPTIMISTIC)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07TransactionModels"

# Best Practices
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BestPractices"

# Isolation Comparison
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07IsolationComparison"

# Deadlock Handling
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07DeadlockHandling"

# Cross-Cache Transactions
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07CrossCacheTransactions"

# Bank Transfer (Challenge)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BankTransfer"

# Transaction Monitor (Challenge)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07TransactionMonitor"
```

## Running Without Maven

```bash
# After running 'mvn package dependency:copy-dependencies'
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07BasicTransactions

# With JVM options
java -Xms512m -Xmx2g -Djava.net.preferIPv4Stack=true \
  -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07BasicTransactions

# Run Transaction Models
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07TransactionModels

# Run Best Practices
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07BestPractices

# Run Isolation Comparison
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07IsolationComparison

# Run Deadlock Handling
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07DeadlockHandling

# Run Cross-Cache Transactions
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07CrossCacheTransactions

# Run Bank Transfer
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07BankTransfer

# Run Transaction Monitor
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab07.Lab07TransactionMonitor
```

## Key Concepts

### Transaction Concurrency Modes

1. **PESSIMISTIC**: Locks are acquired immediately when data is accessed
   - Best when conflicts are likely
   - Prevents conflicts but may cause contention

2. **OPTIMISTIC**: Locks are acquired only at commit time
   - Best for read-heavy workloads
   - Better concurrency but may fail at commit

### Isolation Levels

1. **READ_COMMITTED**: Can see committed changes from other transactions
2. **REPEATABLE_READ**: Same read returns same value within transaction
3. **SERIALIZABLE**: Strictest - transactions execute as if serialized

### Deadlock Prevention

1. Use consistent lock ordering
2. Set appropriate timeouts
3. Implement retry logic with exponential backoff

### Cross-Cache Transactions

- All caches must have `CacheAtomicityMode.TRANSACTIONAL`
- A single transaction can span multiple caches
- Rollback affects all caches atomically

## Common Issues

1. **TransactionOptimisticException**: Retry with new transaction
2. **TransactionDeadlockException**: Use consistent lock ordering
3. **TransactionTimeoutException**: Increase timeout or optimize transaction

## Best Practices

- Keep transactions short
- Do heavy computation outside transactions
- Use batch operations (putAll/getAll)
- Always use try-with-resources
- Monitor transaction metrics
