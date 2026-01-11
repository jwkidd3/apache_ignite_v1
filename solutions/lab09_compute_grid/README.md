# Lab 09: Compute Grid Fundamentals - Solutions

## Overview

This lab covers distributed computing concepts in Apache Ignite, including:
- Basic compute operations (run, call, broadcast)
- Closure-based computing with reducers
- Cluster groups for targeted execution
- MapReduce pattern implementation
- Affinity computing for data locality
- Failover and fault tolerance
- Asynchronous compute operations

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Project Structure

```
lab09_compute_grid/
├── pom.xml
├── README.md
└── src/main/java/com/example/ignite/solutions/lab09/
    ├── Lab09BasicCompute.java      - Exercise 1: Basic compute operations
    ├── Lab09ClosureCompute.java    - Exercise 2: Closure-based computing
    ├── Lab09ClusterGroups.java     - Exercise 3: Targeted execution
    ├── Lab09MapReduce.java         - Exercise 4: MapReduce implementation
    ├── Lab09AffinityCompute.java   - Exercise 5: Affinity computing
    ├── Lab09FailoverCompute.java   - Exercise 6: Failover and fault tolerance
    └── Lab09AsyncCompute.java      - Exercise 7: Asynchronous operations
```

## Quick Start

```bash
# Build
mvn clean compile

# Package (create JAR with dependencies)
mvn clean package

# Run a specific solution
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09BasicCompute"
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
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09BasicCompute" -Dexec.args="" \
  -Djava.net.preferIPv4Stack=true
```

## Running the Solutions

### Exercise 1: Basic Compute
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09BasicCompute"
```

### Exercise 2: Closure Compute
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09ClosureCompute"
```

### Exercise 3: Cluster Groups
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09ClusterGroups"
```

### Exercise 4: MapReduce
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09MapReduce"
```

### Exercise 5: Affinity Compute
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09AffinityCompute"
```

### Exercise 6: Failover Compute
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09FailoverCompute"
```

### Exercise 7: Async Compute
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09AsyncCompute"
```

## Running Without Maven

```bash
# After running 'mvn package dependency:copy-dependencies'
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09BasicCompute

# With JVM options
java -Xms512m -Xmx2g -Djava.net.preferIPv4Stack=true \
  -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09BasicCompute
```

### All Solutions Without Maven

```bash
# Exercise 1: Basic Compute
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09BasicCompute

# Exercise 2: Closure Compute
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09ClosureCompute

# Exercise 3: Cluster Groups
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09ClusterGroups

# Exercise 4: MapReduce
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09MapReduce

# Exercise 5: Affinity Compute
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09AffinityCompute

# Exercise 6: Failover Compute
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09FailoverCompute

# Exercise 7: Async Compute
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab09.Lab09AsyncCompute
```

## Key Concepts

### Basic Compute Operations
- **run()**: Execute IgniteRunnable on a cluster node
- **call()**: Execute IgniteCallable and return result
- **broadcast()**: Execute on all nodes in the cluster
- Automatic load balancing across nodes

### Closures and Reducers
- Apply functions to collections distributedly
- Use reducers for aggregation (sum, max, etc.)
- Functional programming patterns

### Cluster Groups
- **forServers()**: Target server nodes only
- **forClients()**: Target client nodes only
- **forOldest()/forYoungest()**: Target specific nodes
- **forAttribute()**: Target nodes with specific attributes

### MapReduce Pattern
1. **Map Phase**: Split work into jobs, distribute to nodes
2. **Execute**: Each job processes data locally
3. **Reduce Phase**: Aggregate results from all jobs

### Affinity Computing
- **affinityCall()**: Execute where data is located
- **affinityRun()**: Run task on data's primary node
- Eliminates network hops for data access
- Use localPeek() for guaranteed local access

### Failover Computing
- Automatic job retry on node failure
- Configurable failover policies
- Checkpoint-based recovery

### Async Computing
- Non-blocking compute operations
- Future-based result handling
- Parallel task execution

## Best Practices

1. Use affinity computing for data-intensive operations
2. Choose appropriate cluster groups for task routing
3. Keep closures serializable and lightweight
4. Use MapReduce for large-scale data processing
5. Monitor task distribution with cluster metrics
6. Implement proper failover strategies for critical tasks
7. Use async operations for improved throughput

## Common Use Cases

### Basic Compute
- Distributed calculations
- Parallel processing
- Background tasks

### Closures
- Data transformation
- Aggregations
- Filtering operations

### MapReduce
- Log analysis
- Word count
- Statistics computation
- Data aggregation

### Affinity Compute
- Cache entry updates
- Local data processing
- Colocated computations

### Failover Compute
- Mission-critical tasks
- Long-running jobs
- Financial transactions

### Async Compute
- High-throughput processing
- Non-blocking operations
- Parallel task submission
