# Lab 09: Compute Grid Fundamentals - Solutions

## Overview

This lab covers distributed computing concepts in Apache Ignite, including:
- Basic compute operations (run, call, broadcast)
- Closure-based computing with reducers
- Cluster groups for targeted execution
- MapReduce pattern implementation
- Affinity computing for data locality

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
    └── Lab09AffinityCompute.java   - Exercise 5: Affinity computing
```

## Building the Project

```bash
cd lab09_compute_grid
mvn clean compile
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

## Best Practices

1. Use affinity computing for data-intensive operations
2. Choose appropriate cluster groups for task routing
3. Keep closures serializable and lightweight
4. Use MapReduce for large-scale data processing
5. Monitor task distribution with cluster metrics

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
