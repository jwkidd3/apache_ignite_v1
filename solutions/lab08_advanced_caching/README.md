# Lab 08: Advanced Caching Patterns - Solutions

## Overview

This lab covers advanced caching patterns in Apache Ignite, including:
- Near caches for client-side caching
- Expiry policies (Created, Modified, Touched)
- Eviction policies for memory management
- Cache entry processors for atomic operations
- Cache events and continuous queries

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Project Structure

```
lab08_advanced_caching/
├── pom.xml
├── README.md
└── src/main/java/com/example/ignite/solutions/lab08/
    ├── Lab08NearCache.java          - Exercise 1: Near cache configuration
    ├── Lab08ExpiryPolicies.java     - Exercise 2: Expiry policies
    ├── Lab08Eviction.java           - Exercise 3: Eviction policies
    ├── Lab08EntryProcessors.java    - Exercise 4: Cache entry processors
    ├── Lab08CacheEvents.java        - Exercise 5: Cache events
    └── Lab08ContinuousQueries.java  - Exercise 6: Continuous queries
```

## Building the Project

```bash
cd lab08_advanced_caching
mvn clean compile
```

## Running the Solutions

### Exercise 1: Near Cache
```bash
# Start server first
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08NearCache" -Dexec.args="server"

# In another terminal, start client
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08NearCache" -Dexec.args="client"
```

### Exercise 2: Expiry Policies
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08ExpiryPolicies"
```

### Exercise 3: Eviction Policies
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08Eviction"
```

### Exercise 4: Entry Processors
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08EntryProcessors"
```

### Exercise 5: Cache Events
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08CacheEvents"
```

### Exercise 6: Continuous Queries
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08ContinuousQueries"
```

## Key Concepts

### Near Cache
- Client-side cache for frequently accessed data
- Reduces network latency
- Automatic invalidation when server data changes
- Configurable size limits with eviction

### Expiry Policies
1. **CreatedExpiryPolicy**: Expires after creation time
2. **ModifiedExpiryPolicy**: Expires after last modification
3. **TouchedExpiryPolicy**: Expires after last access (read or write)

### Eviction Policies
- **LRU (Least Recently Used)**: Evicts oldest accessed entries
- **FIFO (First In First Out)**: Evicts oldest created entries
- Controls on-heap memory usage

### Entry Processors
- Atomic read-modify-write operations
- Execute logic on the server where data resides
- Reduce network round trips
- Prevent race conditions

### Continuous Queries
- Real-time notifications on cache changes
- Server-side filtering
- Event-driven architecture
- No polling required

## Common Use Cases

### Near Cache
- Frequently read, rarely updated data
- Reference data caching
- Session data for web applications

### Expiry Policies
- Session management
- Token caching
- Temporary data storage

### Entry Processors
- Atomic counters
- Inventory management
- Account balance updates

### Continuous Queries
- Real-time dashboards
- Alert systems
- Data synchronization

## Best Practices

1. Use near caches for read-heavy workloads
2. Choose appropriate expiry based on data lifecycle
3. Size eviction policies based on available memory
4. Use entry processors for atomic operations
5. Apply remote filters to reduce network traffic in continuous queries
