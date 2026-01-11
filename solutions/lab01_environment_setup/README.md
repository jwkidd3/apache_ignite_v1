# Lab 01: Environment Setup and First Ignite Cluster - Solutions

This directory contains the solution files for Lab 1 of the Apache Ignite training course.

## Overview

Lab 1 focuses on:
- Installing and configuring Apache Ignite
- Starting your first Ignite node
- Understanding basic cluster operations
- Verifying installation and cluster connectivity
- Exploring basic cluster metrics

## Solution Files

| File | Description |
|------|-------------|
| `FirstCluster.java` | Exercise 1-3, 5: Creates and starts a simple Ignite node with cluster information display |
| `ClusterMetrics.java` | Exercise 4: Explores cluster-wide and per-node metrics |
| `MultiNodeCluster.java` | Challenge 1: Starts a 3-node cluster in a single JVM |
| `GracefulShutdown.java` | Challenge 2: Implements proper shutdown hooks and graceful termination |
| `HealthCheck.java` | Challenge 3: Node health check utility with thresholds and reporting |

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Building the Project

```bash
cd lab01_environment_setup
mvn clean compile
```

## Running the Solutions

### Exercise 1-3, 5: First Cluster
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.FirstCluster"
```

### Exercise 4: Cluster Metrics
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.ClusterMetrics"
```

### Challenge 1: Multi-Node Cluster
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.MultiNodeCluster"
```

### Challenge 2: Graceful Shutdown
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.GracefulShutdown"
```

### Challenge 3: Health Check
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.HealthCheck"
```

## Key Concepts Demonstrated

1. **IgniteConfiguration**: Basic node configuration including instance name and peer class loading
2. **Ignition.start()**: Starting an Ignite node with try-with-resources for automatic cleanup
3. **ClusterMetrics**: Accessing CPU, memory, job, and thread metrics
4. **Node Discovery**: Automatic cluster formation via multicast discovery
5. **Shutdown Hooks**: Graceful shutdown with Runtime.getRuntime().addShutdownHook()
6. **Event Listeners**: Monitoring cluster topology changes

## Expected Output

When running FirstCluster.java, you should see:
```
Starting Ignite node...
Ignite node started successfully!
Node ID: [UUID]
Node is client: false
Total nodes in cluster: 1
...
```

## Troubleshooting

### Port Already in Use
Ignite uses ports 47500-47509 by default. Ensure these ports are available.

### Multicast Discovery Not Working
If on a network that blocks multicast, you will learn about alternative discovery mechanisms in Lab 2.

### ClassNotFoundException
Verify all dependencies are properly added and Maven has downloaded them:
```bash
mvn dependency:resolve
```
