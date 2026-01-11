# Lab 03: Basic Cache Operations - Solutions

This directory contains the solution files for Lab 3 of the Apache Ignite training course.

## Overview

Lab 3 focuses on:
- Understanding Ignite Cache API fundamentals
- Performing CRUD operations (put, get, remove, replace)
- Working with different cache modes (PARTITIONED, REPLICATED, LOCAL)
- Implementing batch and async operations for efficiency

## Solution Files

| File | Description |
|------|-------------|
| `BasicCacheOps.java` | Exercise 1: Basic CRUD operations (put, get, remove, replace) |
| `CacheModes.java` | Exercise 2: Different cache modes comparison |
| `AsyncOperations.java` | Exercise 3: Synchronous vs asynchronous operations |
| `BatchOperations.java` | Exercise 4: Batch operations (putAll, getAll, removeAll) |
| `AdvancedCacheOps.java` | Optional: EntryProcessors and distributed locking |
| `CacheIteration.java` | Optional: ScanQuery and cache iteration |
| `DataStreamer.java` | Optional: High-speed bulk data loading |
| `BinaryObjects.java` | Optional: Working with Binary Objects |
| `PerformanceBenchmark.java` | Optional: Comprehensive performance measurements |

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Building the Project

```bash
cd lab03_basic_cache_operations
mvn clean compile
```

## Running the Solutions

### Exercise 1: Basic Cache Operations
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.BasicCacheOps"
```

### Exercise 2: Cache Modes
Run in multiple terminals to see distributed behavior:

Terminal 1:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.CacheModes" -Dexec.args="1"
```

Terminal 2:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.CacheModes" -Dexec.args="2"
```

### Exercise 3: Async Operations
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.AsyncOperations"
```

### Exercise 4: Batch Operations
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.BatchOperations"
```

### Optional Exercises

```bash
# Advanced Cache Operations
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.AdvancedCacheOps"

# Cache Iteration
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.CacheIteration"

# DataStreamer
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.DataStreamer"

# Binary Objects
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.BinaryObjects"

# Performance Benchmark
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.PerformanceBenchmark"
```

## Key Concepts Demonstrated

1. **CRUD Operations**: put, get, remove, replace, containsKey
2. **Atomic Operations**: putIfAbsent, getAndPut, getAndRemove
3. **Cache Modes**: PARTITIONED (distributed), REPLICATED (full copy), LOCAL
4. **Batch Operations**: putAll, getAll, removeAll for efficiency
5. **Async Operations**: putAsync, getAsync with futures and callbacks
6. **EntryProcessors**: Server-side processing with invoke()
7. **DataStreamer**: High-throughput bulk loading
8. **Binary Objects**: Schema-less access and field-level operations

## Performance Tips

- Use batch operations (putAll/getAll) for multiple entries
- Use async operations for concurrent workloads
- Use DataStreamer for bulk loading (10,000+ entries)
- Choose appropriate cache mode based on data size and access patterns
- Consider Binary Objects for partial field access

## Troubleshooting

### Cache returns null for existing key
- Ensure you're using the correct cache name
- Verify the key type matches

### Data not distributed as expected
- Check cache mode configuration
- Verify backups setting for PARTITIONED mode

### Performance issues
- Use batch operations instead of individual calls
- Consider async operations for parallelism
- Tune DataStreamer buffer size for bulk loads
